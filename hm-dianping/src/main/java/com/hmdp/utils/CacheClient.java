package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;
    // 缓存重建的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 构建缓存
     * @param key
     * @param value
     * @param time 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    // 设置逻辑过期缓存
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期对象
        RedisData redisData = new RedisData(LocalDateTime.now().plusSeconds(unit.toSeconds(time)), value);
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    // 解决缓存穿透
    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1. 从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3. 存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }
        // 4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        this.set(key, r, time, unit);
        return r;
    }

    // 使用逻辑过期解决缓存击穿
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1. 从 redis 查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在
        if (json != null) {
            if (json.isEmpty()) {
                // 命中空缓存，直接返回
                System.out.println("命中空值缓存！");
                return null;
            }
            // 4. 命中，需要先把json反序列化为对象
            RedisData redisData = JSONUtil.toBean(json, RedisData.class);
            R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
            LocalDateTime expireTime = redisData.getExpireTime();
            // 5. 判断是否过期
            if(expireTime.isAfter(LocalDateTime.now())) {
                // 未过期，直接返回店铺信息'
                System.out.println("逻辑缓存未过期！");
                return r;
            }
            System.out.println("逻辑缓存已过期！");
            // 已过期，需要缓存重建
            String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
            String lockValue = UUID.randomUUID().toString();
            boolean isLock = tryLock(lockKey, lockValue);
            // 6. 判断是否获取锁成功
            if (isLock){
                // 成功，开启独立线程，实现缓存重建
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    try {
                        // 查询数据库
                        R newR = dbFallback.apply(id);
                        // 重建缓存
                        this.setWithLogicalExpire(key, newR, time, unit);
                        System.out.println("逻辑缓存重建成功！");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }finally {
                        // 释放锁
                        unlock(lockKey, lockValue);
                    }
                });
            }
            return r;
        }
        // Redis 没有缓存，查数据库
        R r = dbFallback.apply(id);
        if (r == null) {
            // 数据库无，写空缓存
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 数据库有，写入逻辑过期缓存
        this.setWithLogicalExpire(key, r, time, unit);
        System.out.println("逻辑缓存重建中，返回数据库中商铺信息！");
        // 返回过期的商铺信息
        return r;
    }

    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.实现缓存重建
        // 4.1.获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        String lockValue = UUID.randomUUID().toString();
        R r = null;
        try {
            boolean isLock = tryLock(lockKey, lockValue);
            // 4.2.判断是否获取成功
            if (!isLock) {
                // 4.3.获取锁失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 4.4.获取锁成功，根据id查询数据库
            r = dbFallback.apply(id);
            // 5.不存在，返回错误
            if (r == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 6.存在，写入redis
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 7.释放锁
            unlock(lockKey, lockValue);
        }
        // 8.返回
        return r;
    }

    private boolean tryLock(String lockKey, String lockValue) {
        // 因为设置了过期时间，所以即使下面的删锁方法没成功，redis 也能自动删除
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 60, TimeUnit.SECONDS);
        System.out.println("加锁结果：" + flag);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String lockKey, String lockValue) {
        // 根据传进来的 lockValue 与当前锁的 currentValue 对比，如果一致证明是同一把锁
        // 不一致证明不是同把锁，防止误删
        String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

}

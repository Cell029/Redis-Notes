package com.hmdp.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        /*// 1. 从 redis 中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 2. 判断是否存在，第一层缓存是否命中有效数据
        if (!StringUtil.isNullOrEmpty(shopJson)) { // shopJson 不为 null 且 不为 ""（空字符串）
            // 3. 存在则返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            // 命中缓存
            System.out.println("缓存命中，Shop ID：" + id);
            return Result.ok(shop);
        }
        // 设置了空值，所以要判断请求是否命中空值还是还是不存在缓存中的数据
        // 第二层缓存是否是空字符串（缓存空值）
        if (shopJson != null && shopJson.isEmpty()) {
            // 命中的是缓存的空值，直接返回不存在
            System.out.println("命中空值！");
            return Result.fail("店铺信息不存在！");
        }
        // 4. 不存在则根据 id 查询数据库
        System.out.println("缓存未命中，查询数据库，Shop ID：" + id);
        Shop shop = getById(id);
        // 5. 如果不存在就返回错误信息
        if (shop == null) {
            // 将空值写入 redis
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "",
                    RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺信息不存在！");
        }
        // 6. 存在则把从数据库中查到的数据写入 redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop),
                RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);*/

        return Result.ok(queryWithMutex(id));
    }

    public Shop queryWithMutex(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 从 redis 中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在，第一层缓存是否命中有效数据
        if (StrUtil.isNotBlank(shopJson)) {
            // 3. 存在则返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if (shopJson != null && shopJson.isEmpty()) {
            // 命中的是缓存的空值，直接返回不存在
            System.out.println("命中空值！");
            return null;
        }
        // 4. 实现缓存重构
        // 4.1 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        // 随机生成一个锁的 value，作为锁的唯一标识
        String lockValue = UUID.randomUUID().toString();
        Shop shop = null;
        try {

            boolean isLock = tryLock(lockKey, lockValue);
            // 4.2 判断否获取成功
            if (!isLock) {
                // 4.3 失败，则休眠重试
                System.out.println("加锁失败，休眠一会...");
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 4.4 成功，根据 id 查询数据库
            System.out.println("加锁成功，锁的 value：" + stringRedisTemplate.opsForValue().get(lockKey));
            shop = getById(id);
            // 5. 不存在，返回错误
            if (shop == null) {
                // 将空值写入 redis
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 6. 写入 redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 7. 释放互斥锁，释放后在 resp 客户端中就看不到了
            unlock(lockKey, lockValue);
        }
        return shop;
    }

    @Transactional
    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺 id 不能为空！");
        }
        // 1. 更新数据库
        updateById(shop);
        // 2. 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
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

package com.hmdp.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.Resource;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 缓存重建的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

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

        // 使用互斥锁解决缓存击穿
        // return Result.ok(queryWithMutex(id));

        // 解决缓存穿透
        // cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, shopId -> getById(shopId),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 使用逻辑过期解决缓存击穿
        // Shop shop = queryWithLogicalExpire(id);
        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, shopId -> getById(shopId),
                RedisConstants.LOGIC_EXPIRE_TIME, TimeUnit.SECONDS);
        if (shop == null) {
            return Result.fail("店铺信息不存在！");
        }
        return Result.ok(shop);
    }

    // 使用互斥锁解决缓存击穿
    public Shop queryWithMutex(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 从 redis 中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在，第一层缓存是否命中有效数据
        if (shopJson != null) {
            if (shopJson.isEmpty()) {
                // 命中的是缓存的空值，直接返回不存在
                System.out.println("命中空值！");
                return null;
            }
            // 3. 存在则返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 4. 不存在缓存，则实现缓存重构
        // 获取互斥锁
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
            /*try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
            // 7. 释放互斥锁，释放后在 resp 客户端中就看不到了
            unlock(lockKey, lockValue);
        }
        return shop;
    }

    // 利用逻辑过期结解决缓存击穿
    public Shop queryWithLogicalExpire(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 从 redis 查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否有缓存
        if (shopJson != null) {
            if (shopJson.isEmpty()) {
                // 命中空缓存，直接返回
                System.out.println("命中空值缓存！");
                return null;
            }
            // 3. 命中有效缓存
            RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
            JSONObject data = (JSONObject) redisData.getData();
            Shop shop = JSONUtil.toBean(data, Shop.class);
            LocalDateTime expireTime = redisData.getExpireTime();
            // 4. 判断是否逻辑过期
            if (expireTime.isAfter(LocalDateTime.now())) {
                // 未过期，返回缓存值
                return shop;
            }
            // 5 已过期，需要缓存重建
            // 获取互斥锁
            String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
            String lockValue = UUID.randomUUID().toString();
            boolean isLock = tryLock(lockKey, lockValue);
            // 6. 判断是否获取锁成功
            if (isLock) {
                // 获取锁成功，则重建缓存
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    try {
                        System.out.println("开始重建缓存，店铺ID：" + id);
                        // 开启独立线程，重建缓存
                        this.saveShop2Redis(id, RedisConstants.LOGIC_EXPIRE_TIME);
                        System.out.println("缓存重建完成，店铺ID：" + id);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        unlock(lockKey, lockValue);
                    }
                });
            }
            // 获取锁失败，说明其他线程正在重建，当前线程直接返回旧数据即可
        }
        // Redis 没有缓存，查数据库
        Shop shop = getById(id);
        if (shop == null) {
            // 数据库无，写空缓存
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 数据库有，写入逻辑过期缓存
        saveShop2Redis(id, RedisConstants.LOGIC_EXPIRE_TIME);
        return shop;


        /*if (StrUtil.isEmpty(shopJson)) {
            // 命中空缓存，说明数据库中无数据，直接返回 null
            System.out.println("命中空值！");
            return null;
        }
        // 3. 判断缓存是否存在
        if (StrUtil.isBlank(shopJson)) {
            // 缓存不存在，查询数据库
            Shop shop = getById(id);
            if (shop == null) {
                // 数据库也没有，写空值缓存，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 数据库有，写入缓存并返回
            saveShop2Redis(id, RedisConstants.LOGIC_EXPIRE_TIME); // 设置逻辑过期时间为20秒
            return shop;
        }*/
        /*// 4. 命中，需要先把 json 反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject)  redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5. 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            // 5.1 未过期，直接返回店铺信息
            return shop;
        }
        // 5.2 已过期，需要缓存重建
        // 6. 缓存重建
        // 6.1 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        String lockValue = UUID.randomUUID().toString();
        boolean isLock = tryLock(lockKey, lockValue);
        // 6.2 判断是否获取锁成功
        if (isLock){
            // 获取锁成功，则重建缓存
            CACHE_REBUILD_EXECUTOR.submit( ()->{
                try{
                    System.out.println("开始重建缓存，店铺ID：" + id);
                    // 开启独立线程，重建缓存
                    this.saveShop2Redis(id,RedisConstants.LOGIC_EXPIRE_TIME);
                    System.out.println("缓存重建完成，店铺ID：" + id);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unlock(lockKey, lockValue);
                }
            });
        }*/
        // 6.4 无论是否获取锁，最终返回的都是 Redis 中的旧数据；
        // 新数据由异步线程重建，下次查询才会生效
        // return shop;
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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1. 判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        // 2. 计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        // 3. 查询redis、按照距离排序、分页。结果：shopId、distance
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        // 传入经纬度
                        GeoReference.fromCoordinate(x, y),
                        // 设置距离
                        new Distance(5000),
                        //
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 4. 解析出 id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        // 截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5. 根据 id 查询 Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
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

    public void saveShop2Redis(Long id, Long expireSeconds) {
        // 1. 查询店铺数据
        Shop shop = getById(id);
        // 如果没查到信息
        if (shop == null) {
            // 抛出异常
            throw new IllegalArgumentException("根据 id:" + id + "未查询到店铺信息，无法写入缓存");
        }
        // 2. 封装逻辑过期时间
        RedisData redisData = new RedisData(LocalDateTime.now().plusSeconds(expireSeconds), shop);
        // 3. 写入 redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }
}

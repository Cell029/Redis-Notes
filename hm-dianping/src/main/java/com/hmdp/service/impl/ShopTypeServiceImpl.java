package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopTypeList() {
        // 1. 从 redis 中查询所有商铺类型
        String shopTypeJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_TYPE_LIST_KEY);
        // 2. 判断是否存在
        if (!StringUtil.isNullOrEmpty(shopTypeJson)) {
            // 3. 存在则返回
            List<ShopType> shopTypeList = JSONUtil.toList(JSONUtil.parseArray(shopTypeJson), ShopType.class);
            return Result.ok(shopTypeList);
        }
        // 4. 不存在则查询数据库
        List<ShopType> shopTypeList = query().list();
        if (shopTypeList == null || shopTypeList.isEmpty()) {
            // 5. 如果不存在就返回错误信息
            return Result.fail("店铺类型列表不存在");
        }
        // 6. 存在则把从数据库中查到的数据写入 redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_TYPE_LIST_KEY, JSONUtil.toJsonStr(shopTypeList),
                RedisConstants.CACHE_SHOP_TYPE_LIST_TTL, TimeUnit.MINUTES);
        return Result.ok(shopTypeList);
    }
}








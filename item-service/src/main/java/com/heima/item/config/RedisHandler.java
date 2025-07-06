package com.heima.item.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.item.pojo.Item;
import com.heima.item.pojo.ItemStock;
import com.heima.item.service.IItemService;
import com.heima.item.service.IItemStockService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisHandler implements InitializingBean {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private IItemService itemService;
    @Autowired
    private IItemStockService stockService;
    // Json 处理工具
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化缓存
        // 1. 查询商品信息
        List<Item> itemList = itemService.list();
        // 2. 放入缓存
        for (Item item : itemList) {
            // item序列化为JSON
            String json = MAPPER.writeValueAsString(item);
            // 只在 key 不存在时写入
            Boolean result = redisTemplate.opsForValue().setIfAbsent("item:id:", json);
            // redisTemplate.opsForValue().set("item:id:" + item.getId(), json);
            if (Boolean.TRUE.equals(result)) {
                System.out.println("商品缓存写入：" + item.getId());
            } else {
                System.out.println("商品缓存已存在，跳过：" + item.getId());
            }
        }

        // 3. 查询商品库存信息
        List<ItemStock> stockList = stockService.list();
        // 4. 放入缓存
        for (ItemStock stock : stockList) {
            // item序列化为JSON
            String json = MAPPER.writeValueAsString(stock);
            // 只在 key 不存在时写入
            Boolean result = redisTemplate.opsForValue().setIfAbsent("item:stock:id:", json);

            if (Boolean.TRUE.equals(result)) {
                System.out.println("库存缓存写入：" + stock.getId());
            } else {
                System.out.println("库存缓存已存在，跳过：" + stock.getId());
            }
            // redisTemplate.opsForValue().set("item:stock:id:" + stock.getId(), json);
        }
    }

    public void saveItem(Item item) {
        try {
            String json = MAPPER.writeValueAsString(item);
            redisTemplate.opsForValue().set("item:id:" + item.getId(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteItemById(Long id) {
        redisTemplate.delete("item:id:" + id);
    }
}


package com.heima.item.test;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaffeineTest {

    // 基本用法测试
    @Test
    void testBasicOps() {
        // 创建缓存对象
        Cache<String, String> cache = Caffeine.newBuilder().build();
        // 存数据
        cache.put("name", "张三");
        // 取数据，不存在则返回null
        String name = cache.getIfPresent("name");
        System.out.println("name = " + name);
        // 取数据，不存在则去数据库查询
        String defaultName = cache.get("defaultName", key -> {
            // 这里可以去数据库根据 key 查询 value
            return "李四";
        });
        System.out.println("defaultName = " + defaultName);
    }

    // 基于大小设置驱逐策略
    @Test
    void testEvictByNum() throws InterruptedException {
        // 创建缓存对象
        Cache<String, String> cache = Caffeine.newBuilder()
                // 设置缓存大小上限为 1
                .maximumSize(1)
                .build();
        // 存数据
        cache.put("name1", "张三");
        cache.put("name2", "李四");
        cache.put("name3", "王五");
        // 延迟10ms，给清理线程一点时间
        Thread.sleep(10L);
        // 获取数据
        System.out.println("name1" + cache.getIfPresent("name1"));
        System.out.println("name2: " + cache.getIfPresent("name2"));
        System.out.println("name3: " + cache.getIfPresent("name3"));
    }

    // 基于时间设置驱逐策略
    @Test
    void testEvictByTime() throws InterruptedException {
        // 创建缓存对象
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(1)) // 设置缓存有效期为 10 秒
                .build();
        // 存数据
        cache.put("name", "赵六");
        // 获取数据
        System.out.println("name: " + cache.getIfPresent("name"));
        // 休眠一会儿
        Thread.sleep(1200L);
        System.out.println("name: " + cache.getIfPresent("name"));
    }


}

package com.cell.jedis.test;

import com.cell.jedis.bean.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisStringTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void testString() {
        // 写入一条String数据
        redisTemplate.opsForValue().set("name", "李四");
        // 获取string数据
        Object name = redisTemplate.opsForValue().get("name");
        System.out.println("name = " + name);
    }

    @Test
    void testSaveUser() {
        User user = new User("hahawuwu", 20);
        redisTemplate.opsForValue().set("user", user);
        Object user1 = redisTemplate.opsForValue().get("user");
        System.out.println("user = " + user1);
    }

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Test
    public void testConfig() {
        System.out.println("Redis host: " + redisHost);
    }
}

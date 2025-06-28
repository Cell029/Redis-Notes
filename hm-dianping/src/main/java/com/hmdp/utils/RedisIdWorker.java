package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    // 开始时间戳
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    // 序列号的位数
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号
        // 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 因为 ID 对应的数值上限为 2^64，但是如果起始的 id 不变的话，还是有可能导致超过上限
        // 所以要让它成为动态的，每一天存储 ID 的 key 都不一样，每天的自增计数是独立的
        long count = stringRedisTemplate.opsForValue().increment(RedisConstants.INCREMENT_ID_KEY + keyPrefix + ":" + date);

        // 3. 拼接时间戳与序列号并返回
        // 让时间戳左移 32 位，然后和序列号进行或运算，因为左移后，后面的 32 位全为 0，或运算结果为 count 本身
        return timestamp << COUNT_BITS | count;
    }
}

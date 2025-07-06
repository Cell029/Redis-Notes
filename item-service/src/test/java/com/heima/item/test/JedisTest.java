package com.heima.item.test;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JedisTest {
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        // 1. 建立连接
        jedis = new Jedis("172.23.14.3", 6379);
        // 2. 设置密码
        jedis.auth("123");
        // 3. 选择库
        jedis.select(0);
    }
    // 定义 BigKey 的阈值
    final static int STR_MAX_LEN = 10 * 1024;
    // 其他结构超过 500 个元素视为 BigKey
    final static int HASH_MAX_LEN = 500;

    @Test
    void testScan() {
        int maxLen = 0;
        long len = 0;

        String cursor = "0";
        do {
            // 扫描并获取一部分key
            ScanResult<String> result = jedis.scan(cursor);
            // 记录cursor
            cursor = result.getCursor();
            List<String> list = result.getResult();
            if (list == null || list.isEmpty()) {
                break;
            }
            // 遍历
            for (String key : list) {
                // 判断key的类型
                String type = jedis.type(key);
                switch (type) {
                    case "string":
                        len = jedis.strlen(key);
                        maxLen = STR_MAX_LEN;
                        break;
                    case "hash":
                        len = jedis.hlen(key);
                        maxLen = HASH_MAX_LEN;
                        break;
                    case "list":
                        len = jedis.llen(key);
                        maxLen = HASH_MAX_LEN;
                        break;
                    case "set":
                        len = jedis.scard(key);
                        maxLen = HASH_MAX_LEN;
                        break;
                    case "zset":
                        len = jedis.zcard(key);
                        maxLen = HASH_MAX_LEN;
                        break;
                    default:
                        break;
                }
                if (len >= maxLen) {
                    System.out.printf("Found big key : %s, type: %s, length or size: %d %n", key, type, len);
                }
            }
        } while (!cursor.equals("0"));
    }

    @Test
    void testSetBigKey() {
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i <= 650; i++) {
            map.put("hello_" + i, "world!");
        }
        jedis.hmset("m2", map);
    }

    @Test
    void testBigHash() {
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i <= 100000; i++) {
            map.put("key_" + i, "value_" + i);
        }
        jedis.hmset("test:big:hash", map);
    }

    @Test
    void testBigString() {
        long b = System.currentTimeMillis();
        for (int i = 1; i <= 100000; i++) {
            jedis.set("test:str:key_" + i, "value_" + i);
        }
        long e = System.currentTimeMillis();
        System.out.println("time: " + (e - b));
    }

    @Test
    void testMxx() {
        String[] arr = new String[2000];
        int j;
        long b = System.currentTimeMillis();
        for (int i = 1; i <= 100000; i++) {
            j = (i % 1000) << 1;
            arr[j] = "test:key_" + i;
            arr[j + 1] = "value_" + i;
            if (j == 0) {
                jedis.mset(arr);
            }
        }
        long e = System.currentTimeMillis();
        System.out.println("time: " + (e - b));
    }

    @Test
    void testPipeline() {
        // 创建管道
        Pipeline pipeline = jedis.pipelined();
        long b = System.currentTimeMillis();
        for (int i = 1; i <= 100000; i++) {
            // 放入命令到管道
            pipeline.set("test:key_" + i, "value_" + i);
            if (i % 1000 == 0) {
                // 每放入1000条命令，批量执行
                pipeline.sync();
            }
        }
        long e = System.currentTimeMillis();
        System.out.println("time: " + (e - b));
    }

    @Test
    void testSmallHash() {
        int hashSize = 100;
        Map<String, String> map = new HashMap<>(hashSize);
        for (int i = 1; i <= 100000; i++) {
            int k = (i - 1) / hashSize;
            int v = i % hashSize;
            map.put("key_" + v, "value_" + v);
            if (v == 0) {
                jedis.hmset("test:small:hash_" + k, map);
            }
        }
    }

    @AfterEach
    void tearDown() {
        if (jedis != null) {
            jedis.close();
        }
    }

}

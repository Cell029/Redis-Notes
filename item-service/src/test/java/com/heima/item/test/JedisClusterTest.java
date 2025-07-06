package com.heima.item.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ClusterSlotHashUtil;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.stream.Collectors;

public class JedisClusterTest {
    private JedisCluster jedisCluster;

    @BeforeEach
    void setUp() {
        // 配置连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);
        poolConfig.setMaxWaitMillis(1000);
        HashSet<HostAndPort> nodes = new HashSet<>();
        nodes.add(new HostAndPort("172.23.14.3", 7001));
        nodes.add(new HostAndPort("172.23.14.3", 7002));
        nodes.add(new HostAndPort("172.23.14.3", 7003));
        nodes.add(new HostAndPort("172.23.14.3", 8001));
        nodes.add(new HostAndPort("172.23.14.3", 8002));
        nodes.add(new HostAndPort("172.23.14.3", 8003));
        jedisCluster = new JedisCluster(nodes, (JedisClientConfig) poolConfig);

    }

    @Test
    void testMSet2() {
        Map<String, String> map = new HashMap<>(3);
        map.put("name", "Jack");
        map.put("age", "21");
        map.put("sex", "Male");
        // 对 Map 数据进行分组。根据相同的slot放在一个分组
        // key 就是 slot，value 就是一个组
        Map<Integer, List<Map.Entry<String, String>>> result = map.entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                        // 将 map 中的所有 entry 计算出它们的 key 对应的 slot，然后根据这个 slot 作为新的 key
                        entry -> ClusterSlotHashUtil.calculateSlot(entry.getKey()))
                );
        // 串行的去执行 mset 的逻辑， value 是 List<Map.Entry<String, String>>
        for (List<Map.Entry<String, String>> list : result.values()) {
            String[] arr = new String[list.size() * 2];
            for (int i = 0; i < list.size(); i++) {
                Map.Entry<String, String> e = list.get(i);
                arr[i * 2] = e.getKey();
                arr[i * 2 + 1] = e.getValue();
            }
            jedisCluster.mset(arr);
        }
    }

    @AfterEach
    void tearDown() {
        if (jedisCluster != null) {
            jedisCluster.close();
        }
    }
}

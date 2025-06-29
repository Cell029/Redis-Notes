package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class HmDianPingApplicationTests {
    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Autowired
    private ShopServiceImpl shopService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private RedissonClient redissonClient;



    @Test
    void testSaveShop() {
        shopService.saveShop2Redis(1L, 10L);
    }

    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    @Test
    void testRedisson() throws Exception {
        // 获取锁(可重入)，指定锁的名称
        RLock lock = redissonClient.getLock("anyLock");
        // 尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        // 判断获取锁成功
        if (isLock) {
            try {
                System.out.println("执行业务");
            } finally {
                //释放锁
                lock.unlock();
            }
        }
    }

    private RLock lock;

    @BeforeEach
    void setUp() {
        lock = redissonClient.getLock("order");
    }

    @Test
    void method1() {
        boolean isLock = lock.tryLock();
        if (!isLock) {
            System.out.println("获取锁失败, 1");
            return;
        }
        try {
            System.out.println("获取锁成功, 1");
            method2();
        } finally {
            System.out.println("释放锁, 1");
            lock.unlock();
        }
    }
    void method2() {
        boolean isLock = lock.tryLock();
        if (!isLock) {
            System.out.println("获取锁失败, 2");
            return;
        }
        try {
            System.out.println("获取锁成功, 2");
        } finally {
            System.out.println("释放锁, 2");
            lock.unlock();
        }
    }
}

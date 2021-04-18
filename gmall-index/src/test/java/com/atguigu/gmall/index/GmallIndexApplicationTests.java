package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Random;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private RedissonClient redisson;

    @Test
    void contextLoads() {
        for (int i = 0; i < 10; i++) {
            System.out.println("new Random().nextInt(10) = " + new Random().nextInt(10));
        }
    }

    @Test
    void testRedissonBloomFilter() {
        RBloomFilter<Object> bloomFilter = redisson.getBloomFilter("bloomFilter");
        bloomFilter.tryInit(20l, 0.3);
        for (int i = 1; i < 11; i++) {
            bloomFilter.add(String.valueOf(i));
        }

        for (int i = 1; i < 16; i++) {
            System.out.println(i + "：bloomFilter.contains(String.valueOf(i)) = " + bloomFilter.contains(String.valueOf(i)));
        }
    }

}

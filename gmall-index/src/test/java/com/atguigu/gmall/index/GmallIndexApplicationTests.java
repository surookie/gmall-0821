package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
class GmallIndexApplicationTests {

    @Test
    void contextLoads() {
        for (int i = 0; i < 10; i++) {
            System.out.println("new Random().nextInt(10) = " + new Random().nextInt(10));
        }
    }

}

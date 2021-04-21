package com.atguigu.gmall.cart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@MapperScan("com.atguigu.gmall.cart.mapper")
@SpringBootApplication
@EnableFeignClients
@EnableAsync // 开启声明式异步
public class GmallCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallCartApplication.class, args);
    }

}

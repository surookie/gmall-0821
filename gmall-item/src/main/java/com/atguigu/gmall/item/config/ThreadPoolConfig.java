package com.atguigu.gmall.item.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Description 配置全局线程池
 * @Author rookie
 * @Date 2021/4/16 15:56
 */
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolExecutor getThreadPoolExecutor(
            @Value("${thread.pool.coreSize}") Integer coreSize,
            @Value("${thread.pool.maxSize}") Integer maxSize,
            @Value("${thread.pool.keepAliveTime}") Integer keepAliveTime,
            @Value("${thread.pool.blockQueueSize}") Integer blockQueueSize){
        return new ThreadPoolExecutor(coreSize,maxSize,keepAliveTime, TimeUnit.SECONDS,new ArrayBlockingQueue<>(blockQueueSize));
    }
}
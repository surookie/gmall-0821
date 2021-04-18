package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.cart.exceptionHandler.CartUncaughtExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/18 16:20
 */
@Configuration
public class AsynConfig implements AsyncConfigurer {
    @Autowired
    private CartUncaughtExceptionHandler exceptionHandler;
    /**
     * @description: 返回线程池，规定线程数；
     * @param
     * @return java.util.concurrent.Executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return exceptionHandler;
    }
}
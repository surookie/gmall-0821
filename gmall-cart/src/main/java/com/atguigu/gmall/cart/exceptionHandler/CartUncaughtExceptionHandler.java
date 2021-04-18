package com.atguigu.gmall.cart.exceptionHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/18 16:13
 */
@Component
@Slf4j
public class CartUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
    // 处理未捕获异常
    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("异常信息捕获。方法名：{}， 参数：{}，异常信息：{}", method.getName(), Arrays.asList(objects), throwable.getMessage());
    }
}
package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/14 16:29
 */
@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallPmsApi gmallPmsApi;
    @Autowired
    private RedissonClient redissonClient;


    // 环绕通知必须有一个JoinPoint，返回值为Object，
    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        // 获取方法对象
        Method method = signature.getMethod();
        // 获取方法返回类型
        Class returnType = signature.getReturnType();
        // 获取注解对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        // 获取注解对象的前缀
        String prefix = gmallCache.prefix();
        // 获取方法参数，返回数组，数组的toString（）的返回值是地址，转换为list集合
        List<Object> args = Arrays.asList(joinPoint.getArgs());

        // 拼接前缀和参数，作为缓存的key
        String key = prefix + args;

        // 使用布隆过滤器过滤掉不存在的key
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("index:bloom");
        if (!bloomFilter.contains(key)) {
            return null;
        }

        // 先查询缓存，如果命中，直接返回
        String json = this.redisTemplate.opsForValue().get(key);
        if(StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, returnType);
        }
        // 为了防止缓存击穿，添加分布式锁
        String lock = gmallCache.lock();
        RLock fairLock = redissonClient.getFairLock(lock + args);
        fairLock.lock();
        try {
            // 再查询缓存，如果可以命中，直接返回
            String json1 = this.redisTemplate.opsForValue().get(key);
            if(StringUtils.isNotBlank(json1)){
                return JSON.parseObject(json1, returnType);
            }
            // 执行目标方法，远程调用或者从数据库中获取数据
            Object result = joinPoint.proceed(joinPoint.getArgs());
            //把数据放入缓存
            // 获取缓存时间，为了防止缓存雪崩
            if(result != null){
                int timeout = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
                this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result), timeout, TimeUnit.MINUTES);
            } else {
                // 防止缓存穿透，结果为null也缓存，缓存时间为3min
                this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result), 5, TimeUnit.MINUTES);
            }

            return result;
        } finally {
            fairLock.unlock();
        }
    }

    /*@Before("execution(* com.atguigu.gmall.index.service.*.*(..))")
    public void before(JoinPoint joinPoint){
        Object[] args = joinPoint.getArgs(); // 目标方法参数
        Class<?> aClass = joinPoint.getTarget().getClass();// 目标方法所在类
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    }

    @After("execution(* com.atguigu.gmall.index.service.*.*(..))")
    public void after(JoinPoint joinPoint){
        Object[] args = joinPoint.getArgs(); // 目标方法参数
        Class<?> aClass = joinPoint.getTarget().getClass();// 目标方法所在类
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    }

    @AfterReturning(value = "execution(* com.atguigu.gmall.index.service.*.*(..))",returning = "result")
    public void afterReturning(JoinPoint joinPoint,Object result){
        Object[] args = joinPoint.getArgs(); // 目标方法参数
        Class<?> aClass = joinPoint.getTarget().getClass();// 目标方法所在类
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    }

    @AfterThrowing(value = "execution(* com.atguigu.gmall.index.service.*.*(..))",throwing = "e")
    public void afterThrowing (JoinPoint joinPoint, Exception e){
        Object[] args = joinPoint.getArgs(); // 目标方法参数
        Class<?> aClass = joinPoint.getTarget().getClass();// 目标方法所在类
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    }*/




    /*@Pointcut("execution(* com.atguigu.gmall.index.service.*.*(..))")
    public void pointCut(){

    }*/
}
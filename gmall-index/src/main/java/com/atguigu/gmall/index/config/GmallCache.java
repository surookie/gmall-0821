package com.atguigu.gmall.index.config;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * @author rookie
 * @Title:
 * @Description: 自定义缓存，并且加上分布式锁
 * @date 2021/4/14 15:44
 */
@Target({ElementType.METHOD}) // 注解作用到方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时注解
//@Inherited // 被继承
@Documented // 加入文档
public @interface GmallCache {

    /**
     * 设置缓存前缀
     * 缓存key：prefix + 方法参数
     */
    String prefix() default "";

    /**
     * 缓存时间, 默认缓存5分钟
     */
    int timeout() default 5;

    /**
     * 防止缓存雪崩，
     * 给缓存指定随机值范围
     */
    int random() default 5;

    /**
     * 防止缓存击穿，
     * 给缓存指定分布式锁前缀
     */
    String lock() default "lock:";

}

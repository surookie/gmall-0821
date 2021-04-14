package com.atguigu.gmall.index.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Description 设置可重入锁，防止死锁发生
 * @Author rookie
 * @Date 2021/4/13 20:45
 */
@Component
@Slf4j
public class DistributedLock {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private Timer timer;

    /**
     * @param lockName
     * @param uuid
     * @param expire
     * @return java.lang.Boolean
     * @description: 加锁
     */
    public Boolean tryLock(String lockName, String uuid, Integer expire) {
        String script = "if(redis.call('exists',KEYS[1]) == 0 or redis.call('hexists',KEYS[1], ARGV[1]) == 1) then " +
                "redis.call('hincrby',KEYS[1], ARGV[1], 1) " +
                "redis.call('expire',KEYS[1], ARGV[2]) " +
                "return 1 " +
                "else " +
                "return 0 " +
                "end";
        //如果返回为false,代表未抢到锁，则需要进行递归重试
        if (!redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString())) {
            try {
                Thread.sleep(50);
                tryLock(lockName, uuid, expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 开启定时任务子线程定时续期
        renewExpire(lockName, uuid, expire);
        // 返回为true，表示抢到锁了。
        return true;
    }

    /**
     * @param lockName
     * @param uuid
     * @return void
     * @description: 解锁
     */
    public void unlock(String lockName, String uuid) {
        String script = "if(redis.call('hexists',KEYS[1],ARGV[1]) == 0) then " +
                "return nil " +
                "elseif(redis.call('hincrby',KEYS[1],ARGV[1],-1) == 0) then " +
                "return redis.call('hdel',KEYS[1],ARGV[1]) " +
                "else return 0 " +
                "end";

        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);

        // 如果返回为null，则表示锁不存在，或者是在借别人的锁
        // 如果返回为0，则表示锁解开了一次
        // 如果返回为1，则表示解锁成功
        if (result == null) {
            log.error("要解的锁不存在或者在解别人的锁，锁名：{}，uuid：{}", lockName, uuid);
        } else if(result == 1) {
            // 当锁释放成功后，关闭定时任务
            timer.cancel();
        }
    }

    public void renewExpire(String lockName, String uuid, Integer expire) {
        String script = "if(redis.call('hexists',KEYS[1],ARGV[1]) == 1) then " +
                "return redis.call('expire',KEYS[1],ARGV[2]) " +
                "else return 0 " +
                "end";
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
            }
        }, expire * 1000 / 3, expire * 1000 / 3);
    }
}
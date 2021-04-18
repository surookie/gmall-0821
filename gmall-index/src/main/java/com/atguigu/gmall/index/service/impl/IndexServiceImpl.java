package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Description 首页分页业务逻辑实现
 * @Author rookie
 * @Date 2021/4/12 15:07
 */
@Service
public class IndexServiceImpl implements IndexService {

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";


    @Override
    public List<CategoryEntity> queryLv1Categories() {
        return this.gmallPmsClient.queryCategory(0l).getData();
    }

    /**
     * @description: 使用aop自定义注解的方式，减少冗余代码，将业务逻辑还原到业务本身，对比
     * queryLv2CategoriesWithSubsByPid与queryLv2CategoriesWithSubsByPid2
     * @param pid
     * @return java.util.List<com.atguigu.gmall.pms.entity.CategoryEntity>
     */
    @GmallCache(prefix = KEY_PREFIX, timeout = 43200, random = 7200, lock = KEY_PREFIX +"lock:")
    @Override
    public List<CategoryEntity> queryLv2CategoriesWithSubsByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> responseVo = this.gmallPmsClient.queryLv2CategoriesWithSubsByPid(pid);
        return responseVo.getData();
    }

    public List<CategoryEntity> queryLv2CategoriesWithSubsByPid2(Long pid) {
        // 先查询缓存，有则返回，
        String categoryEntities = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(categoryEntities)) {
            return JSON.parseArray(categoryEntities, CategoryEntity.class);
        }

        // 防止缓存穿透，添加分布式锁 避免资源锁冲突，模块名加实例贾锁名加pid
        RLock lock = redissonClient.getLock(KEY_PREFIX + "lock:" + pid);
        lock.lock();

        // 再次查询缓存，在请求等待过程中，可能已经有其他请求已经把资源放入缓存中
        String categoryEntities2 = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(categoryEntities2)) {
            return JSON.parseArray(categoryEntities2, CategoryEntity.class);
        }

        // 防止缓存穿透，数据即使为空也缓存进去
        List<CategoryEntity> categoryEntityList = this.gmallPmsClient.queryLv2CategoriesWithSubsByPid(pid).getData();
        if (CollectionUtils.isEmpty(categoryEntityList)) {
            redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntityList), 5, TimeUnit.MINUTES);
        } else {
            // 为了防止缓存雪崩，过期时间上加上随机过期时间
            redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntityList), 30 + new Random().nextInt(10), TimeUnit.DAYS);
        }
        // 解锁
        lock.unlock();
        // 无则远程调用数据库，获取数据，放入缓存
        return categoryEntityList;
    }

    @Override
    public void testLock() {
        // 加锁
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        try {
            // 执行业务代码
            String number = redisTemplate.opsForValue().get("number");
            if (StringUtils.isEmpty(number)) {
                return;
            }
            int i = Integer.parseInt(number);
            redisTemplate.opsForValue().set("number", String.valueOf(++i));
        } finally {
            // 解锁
            lock.unlock();
        }

    }

    @Override
    public void testSemaphore() {
        RSemaphore semaphore = redissonClient.getSemaphore("semaphore");
        semaphore.trySetPermits(3);
        for (int i = 0; i < 6; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        semaphore.acquire();
                        System.out.println(Thread.currentThread().getName() + "抢到车位了。。。");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    semaphore.release();
                    System.out.println(Thread.currentThread().getName() + "把车开走了。。。");
                }
            });
        }
    }

    @Override
    public void stuCountDownLock() {
        RCountDownLatch countDown = redissonClient.getCountDownLatch("countDown");
        // TODO 业务逻辑
        System.out.println("一位同学走了。。。");
        countDown.countDown();
    }

    @Override
    public void testReadLock() {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(5, TimeUnit.SECONDS);

    }

    @Override
    public void testWriterLock() {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(5, TimeUnit.SECONDS);
    }

    @Override
    public void monitorCountDownLock() {
        RCountDownLatch countDown = redissonClient.getCountDownLatch("countDown");
        countDown.trySetCount(4); // 等待四个线程结束，执行后续业务
        System.out.println("班长要关门了。。。。");
        try {
            countDown.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO 业务逻辑
        System.out.println("班长已经锁门了。。。。");
    }

    public void testSubLock(String uuid) {
        this.distributedLock.tryLock("lock", uuid, 30);
        System.out.println("测试可重入锁");
        this.distributedLock.unlock("lock", uuid);
    }

    /*public void testLock3() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30);
        if (lock) {
            // 执行业务代码
            String number = redisTemplate.opsForValue().get("number");
            if (StringUtils.isEmpty(number)) {
                return;
            }

            int i = Integer.parseInt(number);
            redisTemplate.opsForValue().set("number", String.valueOf(++i));

            //try {
            //    TimeUnit.SECONDS.sleep(1000);
            //} catch (InterruptedException e) {
            //e.printStackTrace();
            //}
            //this.testSubLock(uuid);
            this.distributedLock.unlock("lock", uuid);
        }
    }*/

    /*public void testLock2() {
        String uuid = UUID.randomUUID().toString();
        // 获取锁, 为了防止死锁（服务器宕机原因，未能释放锁，导致谁都获取不到），为锁加上过期时间
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        if(!lock){
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testLock();
        } else {
            // 为锁加入过期时间,该方式不能保证原子性，不能完全防止死锁
            redisTemplate.expire("lock", 3, TimeUnit.SECONDS);
            // 执行业务代码
            String number = redisTemplate.opsForValue().get("number");
            if(StringUtils.isEmpty(number)){
                return;
            }
            int i = Integer.parseInt(number);
            redisTemplate.opsForValue().set("number", String.valueOf(++i));
            // 防误删锁:问题，不能保证判断和删除的原子性，解决：使用lua脚本打包执行（判断与删除一体）
            String script = "if(redis.call('get',KEYS[1]) == ARGV[1]) then return redis.call('del',KEYS[1]) else return 0 end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
            // 释放锁
            //if(StringUtils.equals(uuid, redisTemplate.opsForValue().get("lock"))){
            //    redisTemplate.delete("lock");
            //}
        }
    }*/
}
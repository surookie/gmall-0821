package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    public static final String LOCK_PREFIX = "stock:lock:";
    public static final String KEY_PREFIX = "stock:info:";

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken) {
        if (CollectionUtils.isEmpty(lockVos)) {
            throw new OrderException("您没有要购买的商品。");
        }

        // 遍历所有商品，验库存，锁库存
        lockVos.forEach(lockVo -> checkLock(lockVo));

        // 只要有一个商品锁定失败，就要把锁定成功商品解锁
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())) {
            lockVos.stream().filter(SkuLockVo::getLock).forEach(lockVo -> {
                this.wareSkuMapper.unlock(lockVo.getSkuId(), lockVo.getCount());
            });

            // 相应锁定状态
            return lockVos;
        }

        // 如果所有商品都锁定成功的情况下，需要锁定信息缓存到redis里面，以方便以后解锁库存或减库存
        // 以唯一标识前缀+orderToken为key，lockVos的json字符串为value
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

        // 锁定成功后，定时解锁库存
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.ttl", orderToken);

        return null;
    }

    private void checkLock(SkuLockVo lockVo) {
        RLock fairLock = this.redissonClient.getFairLock(LOCK_PREFIX + lockVo.getSkuId());
        fairLock.lock();
        try {
            Integer count = lockVo.getCount();
            // 验库存, 获取满足条件的仓库
            List<WareSkuEntity> wareSkuEntities = wareSkuMapper.check(lockVo.getSkuId(), count);
            // 没有一个满足要求，这里就验库存失败
            if (CollectionUtils.isEmpty(wareSkuEntities)) {
                lockVo.setLock(false);
                return;
            }

            // TODO 大数据分析，获取就近的仓库。这里求方便，取第一个仓库
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);

            // 锁库存: 更新
            Long wareSkuId = wareSkuEntity.getId();
            if (this.wareSkuMapper.lock(wareSkuId, count) == 1) {
                lockVo.setLock(true);
                lockVo.setWareSkuId(wareSkuId);
            }

        } finally {
            fairLock.unlock();
        }


    }

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

}
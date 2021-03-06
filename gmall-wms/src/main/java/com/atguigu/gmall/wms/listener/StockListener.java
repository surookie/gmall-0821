package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/21 16:41
 */
@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    public static final String KEY_PREFIX = "stock:info:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "OMS_UNLOCK_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.disable"}
    ))
    public void unlockStock(String orderToken, Channel channel, Message message){
        try {
            if(StringUtils.isBlank(orderToken)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 获取锁定库存的缓存信息
            String skuLockJson = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
            if(StringUtils.isBlank(skuLockJson)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            // 解锁库存
            List<SkuLockVo> skuLockVos = JSON.parseArray(skuLockJson, SkuLockVo.class);
            skuLockVos.forEach(lockVo -> {
                this.wareSkuMapper.unlock(lockVo.getWareSkuId(), lockVo.getCount());
            });

            // 解锁库存之后，立刻删除锁定的库存信息，防止重复解锁
            this.redisTemplate.delete(KEY_PREFIX + orderToken);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "OMS_MINUS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minusStock(String orderToken, Channel channel, Message message){
        try {
            if(StringUtils.isBlank(orderToken)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 获取锁定库存的缓存信息
            String skuLockJson = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
            if(StringUtils.isBlank(skuLockJson)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            // 解锁库存
            List<SkuLockVo> skuLockVos = JSON.parseArray(skuLockJson, SkuLockVo.class);
            skuLockVos.forEach(lockVo -> {
                this.wareSkuMapper.minus(lockVo.getWareSkuId(), lockVo.getCount());
            });

            // 解锁库存之后，立刻删除锁定的库存信息，防止重复解锁
            this.redisTemplate.delete(KEY_PREFIX + orderToken);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.fegin.GmallPmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/19 13:46
 */
@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:info:";

    private static final String PRICE_PREFIX = "cart:price:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_PRICE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        try {
            if (spuId == null) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            List<SkuEntity> skuEntities = this.pmsClient.querySkuBySpuId(spuId).getData();

            if (CollectionUtils.isEmpty(skuEntities)) {
                return;
            }

            skuEntities.forEach(skuEntity -> {
                // 如果该商品已经加入购物车，才会加入价格缓存
                if(redisTemplate.hasKey(PRICE_PREFIX + skuEntity.getId())){
                    redisTemplate.opsForValue().set(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
                }
            });

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_DELETE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.delete"}
    ))
    public void deleteCartListener(Map<String, Object> map, Channel channel, Message message) throws IOException {
        try {
            if(CollectionUtils.isEmpty(map)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 获取用户Id和skuIds
            String userId = map.get("userId").toString();
            List<String> skuIds = JSON.parseArray(map.get("skuIds").toString(), String.class);

            // 获取对应用户购物车的参数
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
            // 删除对应订单的购物车记录
            hashOps.delete(skuIds.toArray());

            // 消费消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                //
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
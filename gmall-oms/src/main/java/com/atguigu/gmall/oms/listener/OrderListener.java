package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Description 异步回滚，取消订单和解锁库存 （TTC 补偿协议）
 * @Author rookie
 * @Date 2021/4/21 16:23
 */
@Component
public class OrderListener {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "OMS_DISABLE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.disable"}
    ))
    public void disableOrder(String orderToken, Channel channel, Message message) {
        try {
            if (StringUtils.isBlank(orderToken)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 更新订单为无效订单
            this.orderMapper.updateStatus(orderToken, 0, 5);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "OMS_SUCCESS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.success"}
    ))
    public void successOrder(String orderToken, Channel channel, Message message) {
        try {
            if (StringUtils.isBlank(orderToken)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 更新订单为待发货状态
            if (this.orderMapper.updateStatus(orderToken, 0, 1) == 1) {
                // 发送消息给wms减库存
                this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.minus", orderToken);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = {"ORDER_DEAD_QUEUE"})
    public void closeOrder(String orderToken, Channel channel, Message message) {
        try {
            if (StringUtils.isBlank(orderToken)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 更新订单状态为关闭状态
            if (this.orderMapper.updateStatus(orderToken, 0, 4) == 1) {
                // 发送消息通知库存管理系统，解锁库存
                rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.disable", orderToken);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
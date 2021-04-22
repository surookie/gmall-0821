package com.atguigu.gmall.wms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/21 15:43
 */
@Slf4j
@Configuration
public class RabbitConfig {

    @Autowired
    public RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        this.rabbitTemplate.setConfirmCallback((@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) -> {
            if (ack) {
                log.error("消息没有到达交换机，失败原因：{}", cause);
            }
        });
        this.rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey) -> {
            log.error("消息没有到达队列，交换机：{}，路由键：{}，消息内容：{}", exchange, routingKey, new String(message.getBody()));
        });
    }

    /**
     * 定义业务交换机： ORDER_EXCHANGE
     *
     */

    /**
     * 定义延时队列 STOCK_TTL_QUEUE
     * 配置参数：
     * *  x-message-ttl： 100000（毫秒）
     * *  x-dead-letter-exchange :ORDER_EXCHANGE
     * *  x-dead-routing-key: order.disable
     */
    @Bean
    public Queue ttlQueue() {
        return QueueBuilder.durable("STOCK_TTL_QUEUE")
                .withArgument("x-message-ttl", 100000)
                .withArgument("x-dead-letter-exchange", "ORDER_EXCHANGE")
                .withArgument("x-dead-letter-routing-key", "order.disable")
                .build();
    }

    /**
     * 定义延时队列绑定到业务交换机 stock.ttl
     */
    @Bean
    public Binding ttlBinging() {
        return new Binding("STOCK_TTL_QUEUE", Binding.DestinationType.QUEUE, "ORDER_EXCHANGE", "stock.ttl", null);
    }
    /**
     * 死信交换机：ORDER_EXCHANGE
     */


    /**
     * 死信队列 ：OMS_UNLOCK_QUEUE
     */

    /**
     * 死信队列绑定到死信交换机 order.disable
     */

    @Bean
    public Binding deadBinding() {
        return new Binding("OMS_UNLOCK_QUEUE", Binding.DestinationType.QUEUE, "ORDER_EXCHANGE", "order.disable", null);
    }
}
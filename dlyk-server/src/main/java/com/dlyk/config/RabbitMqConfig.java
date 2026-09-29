package com.dlyk.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 *
 * <p>用主题交换机把业务事件与消费方解耦：发布方只关心"发生了什么"，
 * 需要感知的消费方各自绑定自己的队列，新增消费方不需要改动发布方代码。
 *
 * <p>队列声明为 durable 且不使用自动删除，保证消费方短暂离线时消息不丢。
 *
 * @author ShigureYukina
 */
@Configuration
@ConditionalOnProperty(name = "dlyk.mq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

    /** CRM 业务事件主题交换机 */
    public static final String CRM_TOPIC_EXCHANGE = "dlyk.crm.topic";

    /** 客户转化事件队列 */
    public static final String CUSTOMER_CONVERTED_QUEUE = "dlyk.customer.converted.queue";

    /** 客户转化事件路由键 */
    public static final String CUSTOMER_CONVERTED_ROUTING_KEY = "customer.converted";

    @Bean
    public TopicExchange crmTopicExchange() {
        return new TopicExchange(CRM_TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue customerConvertedQueue() {
        return QueueBuilder.durable(CUSTOMER_CONVERTED_QUEUE).build();
    }

    @Bean
    public Binding customerConvertedBinding(Queue customerConvertedQueue, TopicExchange crmTopicExchange) {
        return BindingBuilder.bind(customerConvertedQueue)
                .to(crmTopicExchange)
                .with(CUSTOMER_CONVERTED_ROUTING_KEY);
    }

    /**
     * 事件以 JSON 序列化，避免默认 JDK 序列化带来的跨语言与可读性问题。
     * Spring Boot 检测到容器中存在唯一的 MessageConverter 时，
     * 会同时用于 RabbitTemplate 与 @RabbitListener。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

package com.dlyk.mq;

import com.dlyk.config.RabbitMqConfig;
import com.dlyk.event.CustomerConvertedEvent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 客户转化事件发布方
 *
 * <p>关键约束：**事件发布是旁路能力，不能影响主链路**。
 * 因此这里把发送异常全部吞掉并记录日志 —— MQ 抖动时业务照常完成，
 * 数据一致性由对账兜底，而不是让用户看到"转化失败"。
 *
 * @author ShigureYukina
 */
@Component
public class CustomerConvertedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CustomerConvertedEventPublisher.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${dlyk.mq.enabled:true}")
    private boolean mqEnabled;

    public void publish(CustomerConvertedEvent event) {
        if (!mqEnabled) {
            log.debug("MQ 已关闭，跳过事件发布。clueId={}", event.getClueId());
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.CRM_TOPIC_EXCHANGE,
                    RabbitMqConfig.CUSTOMER_CONVERTED_ROUTING_KEY,
                    event);
            log.debug("已发布客户转化事件: {}", event);
        } catch (Exception e) {
            log.error("发布客户转化事件失败，不影响主流程。clueId={}", event.getClueId(), e);
        }
    }
}

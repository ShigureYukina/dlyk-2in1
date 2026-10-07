package com.dlyk.mq;

import com.dlyk.config.RabbitMqConfig;
import com.dlyk.event.ClueChangedEvent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 线索变更事件发布方（供 ES 检索同步）
 *
 * <p>与 {@link CustomerConvertedEventPublisher} 同一条约束：**同步是旁路能力，不能影响主链路**。
 * 发布异常全部吞掉 —— MQ 或 ES 抖动时业务照常完成，索引缺口由管理端
 * {@code POST /api/es/reindex} 全量重建兜底，而不是让新增线索报错。
 *
 * @author ShigureYukina
 */
@Component
public class ClueChangedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClueChangedEventPublisher.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${dlyk.mq.enabled:true}")
    private boolean mqEnabled;

    @Value("${dlyk.es.enabled:false}")
    private boolean esEnabled;

    public void publishChanged(Integer clueId) {
        publish(clueId, RabbitMqConfig.CLUE_CHANGED_ROUTING_KEY);
    }

    public void publishDeleted(Integer clueId) {
        publish(clueId, RabbitMqConfig.CLUE_DELETED_ROUTING_KEY);
    }

    private void publish(Integer clueId, String routingKey) {
        if (!mqEnabled || !esEnabled) {
            log.debug("MQ 或 ES 检索未开启，跳过线索变更事件发布。clueId={}", clueId);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.CRM_TOPIC_EXCHANGE,
                    routingKey,
                    new ClueChangedEvent(clueId, new Date()));
            log.debug("已发布线索变更事件: clueId={} routingKey={}", clueId, routingKey);
        } catch (Exception e) {
            log.error("发布线索变更事件失败，不影响主流程。clueId={}", clueId, e);
        }
    }
}

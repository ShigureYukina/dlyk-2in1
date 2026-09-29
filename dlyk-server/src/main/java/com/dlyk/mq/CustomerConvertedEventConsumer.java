package com.dlyk.mq;

import com.dlyk.config.RabbitMqConfig;
import com.dlyk.event.CustomerConvertedEvent;
import com.dlyk.result.ReconcileResult;
import com.dlyk.service.ReconcileService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户转化事件消费方：对刚变更的线索做一次增量对账
 *
 * <p>与 XXL-Job 的全量对账形成"事件驱动 + 周期兜底"：
 * 事件驱动让问题在秒级被发现，周期对账兜住消息丢失与历史脏数据。
 * 两者都写同一张对账表，因此告警与排查入口统一。
 *
 * @author ShigureYukina
 */
@Component
@ConditionalOnProperty(name = "dlyk.mq.enabled", havingValue = "true", matchIfMissing = true)
public class CustomerConvertedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CustomerConvertedEventConsumer.class);

    @Resource
    private ReconcileService reconcileService;

    @RabbitListener(queues = RabbitMqConfig.CUSTOMER_CONVERTED_QUEUE)
    public void onCustomerConverted(CustomerConvertedEvent event) {
        if (event == null || event.getClueId() == null) {
            log.warn("收到无效的客户转化事件，已忽略: {}", event);
            return;
        }
        ReconcileResult result = reconcileService.reconcileClues(List.of(event.getClueId()));
        if (result.getTotalIssues() > 0) {
            log.warn("客户转化事件触发的增量对账发现不一致 | clueId={} | {}", event.getClueId(), result.summary());
        }
    }
}

package com.dlyk.mq;

import com.dlyk.config.RabbitMqConfig;
import com.dlyk.event.ClueChangedEvent;
import com.dlyk.mapper.TClueMapper;
import com.dlyk.model.TClue;
import com.dlyk.search.ClueSearchService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 线索变更事件消费方：维护 ES 检索副本。
 *
 * <p>同一条队列承接两种动作，用路由键区分（clue.changed / clue.deleted），
 * 事件体只带 clueId —— "文档长什么样"由消费端查库决定，发布方不需要知道索引结构。
 *
 * <p>需要同时满足 dlyk.es.enabled 与 dlyk.mq.enabled：
 * ES 关闭时没有索引可同步，MQ 关闭时连监听容器都不该起。
 *
 * @author ShigureYukina
 */
@Component
@ConditionalOnProperty(name = {"dlyk.es.enabled", "dlyk.mq.enabled"}, havingValue = "true")
public class ClueEsSyncConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClueEsSyncConsumer.class);

    @Resource
    private ClueSearchService clueSearchService;

    @Resource
    private TClueMapper tClueMapper;

    @RabbitListener(queues = RabbitMqConfig.ES_SYNC_QUEUE)
    public void onClueChanged(ClueChangedEvent event,
                              @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        if (event == null || event.getClueId() == null) {
            log.warn("收到无效的线索变更事件，已忽略: {}", event);
            return;
        }
        if (RabbitMqConfig.CLUE_DELETED_ROUTING_KEY.equals(routingKey)) {
            clueSearchService.deleteById(event.getClueId());
            return;
        }
        List<TClue> rows = tClueMapper.selectCluesByIds(List.of(event.getClueId()));
        TClue clue = rows.isEmpty() ? null : rows.get(0);
        if (clue == null || (clue.getDeleted() != null && clue.getDeleted() != 0)) {
            // 线索已不存在或已被逻辑删除：索引文档也应移除，避免搜出脏数据
            clueSearchService.deleteById(event.getClueId());
            return;
        }
        clueSearchService.indexClue(clue);
    }
}

package com.dlyk.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 线索变更事件（新增/编辑后发布）
 *
 * <p>消费方目前是 ES 同步（{@code mq.ClueEsSyncConsumer}）：MySQL 为主库、ES 为检索副本，
 * 写入路径只管发布"这条线索变了"，由消费端决定重索引还是删除文档。
 * 逻辑删除走独立的 {@code clue.deleted} 路由键，事件体不区分动作。
 *
 * @author ShigureYukina
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClueChangedEvent implements Serializable {

    /** 线索ID */
    private Integer clueId;

    /** 事件发生时间 */
    private Date occurredAt;
}

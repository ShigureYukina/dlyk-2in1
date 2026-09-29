package com.dlyk.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户转化成功事件
 *
 * <p>在线索转客户事务提交后发布，用于把"转化后的数据校验"从同步主链路
 * 剥离到异步消费端，避免主链路承担额外耗时。
 *
 * @author ShigureYukina
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerConvertedEvent implements Serializable {

    /** 线索ID */
    private Integer clueId;

    /** 事件发生时间 */
    private Date occurredAt;
}

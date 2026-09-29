package com.dlyk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 对账记录表
 * t_reconcile_record
 */
@Data
@TableName("t_reconcile_record")
public class TReconcileRecord implements Serializable {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 对账类型 */
    private String bizType;

    /** 业务键，如 clueId / customerId */
    private String bizKey;

    /** 差异详情 */
    private String detail;

    /** 执行分片序号 */
    private Integer shardIndex;

    /** 分片总数 */
    private Integer shardTotal;

    /** 发现时间 */
    private Date createTime;
}

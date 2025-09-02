package com.dlyk.query;

import lombok.Data;

import java.util.Date;

/**
 * 交易备注查询对象
 */
@Data
public class TranRemarkQuery extends BaseQuery {
    private Integer id;
    private Integer tranId;
    private String noteContent;
    private Integer noteWay;
    private Date noteTime;
}
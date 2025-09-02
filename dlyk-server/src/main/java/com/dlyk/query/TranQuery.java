package com.dlyk.query;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 交易查询对象
 */
@Data
public class TranQuery extends BaseQuery {
    private Integer id;
    private Integer customerId;
    private String tranNo;
    private BigDecimal money;
    private String expectedDate;
    private Integer stage;
    private String description;
    private Integer ownerId;
    private Date nextContactTime;
}
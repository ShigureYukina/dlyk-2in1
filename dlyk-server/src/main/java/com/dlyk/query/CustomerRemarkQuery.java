package com.dlyk.query;

import lombok.Data;

/**
 * 客户备注查询对象
 */
@Data
public class CustomerRemarkQuery extends BaseQuery {
    private Integer id;
    private Integer customerId;
    private String noteContent;
    private Integer noteWay;
}
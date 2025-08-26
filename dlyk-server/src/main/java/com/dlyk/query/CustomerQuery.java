package com.dlyk.query;

import lombok.Data;

import java.util.Date;

/** copy by ShigureYukina,from 2025/8/26-下午10:10 */
@Data
public class CustomerQuery extends BaseQuery {
    private Integer clueId;
    private Integer product;
    private String description;
    private Date nextContactTime;
}

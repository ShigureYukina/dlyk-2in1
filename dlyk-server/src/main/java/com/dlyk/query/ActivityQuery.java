package com.dlyk.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/** copy by ShigureYukina,from 2025/8/21-下午3:47 */
@Data
public class ActivityQuery extends BaseQuery {
    private Integer id;

    private Integer ownerId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private BigDecimal cost;

    private String name;

    private String description;


}

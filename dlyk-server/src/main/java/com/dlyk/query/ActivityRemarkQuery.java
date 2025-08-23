package com.dlyk.query;

import lombok.Data;

/** copy by ShigureYukina,from 2025/8/22-下午10:22 */
@Data
public class ActivityRemarkQuery extends BaseQuery {
    private Integer id;
    private Integer activityId;
    private String noteContent;


}

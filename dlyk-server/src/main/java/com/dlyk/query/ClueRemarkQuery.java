package com.dlyk.query;

import lombok.Data;

/** copy by ShigureYukina,from 2025/8/26-下午6:18 */
@Data
public class ClueRemarkQuery extends BaseQuery {
    private Integer id;
    private Integer clueId;
    private String noteContent;
    private Integer noteWay;

}
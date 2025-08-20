package com.dlyk.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** copy by ShigureYukina,from 2025/8/18-下午2:55 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BaseQuery {
    private String token;
    private String filterSQL;// 过滤条件
}

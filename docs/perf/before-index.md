# 慢查询探针报告（before-index）

数据量：
t_clue=356352 t_customer=356353 t_tran=356354

| 探针 | 说明 | EXPLAIN type | 耗时(ms) |
| --- | --- | --- | --- |
| P1 | 线索存活数（deleted 谓词） | ALL | 40.6 |
| P2 | 手机号唯一性校验 checkPhone | ALL | 41.1 |
| P3 | 列表联查 深分页 offset=300000 | ALL,eq_ref,eq_ref,eq_ref,eq_ref,eq_ref,eq_ref,eq_ref,ALL | 403.3 |
| P4 | 列表联查 游标分页 id<56352 | range,eq_ref,eq_ref,eq_ref,eq_ref,eq_ref,eq_ref,eq_ref,eq_ref | 0.4 |
| P5 | 对账-重复客户线索 | index | 465.6 |
| P6 | 交易列表与客户联查 | ALL,ref | 292.9 |
| P7 | 活动名称前导通配搜索 | ALL | 0.3 |

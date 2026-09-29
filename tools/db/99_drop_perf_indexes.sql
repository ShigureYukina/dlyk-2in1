-- ============================================================
-- 99 回退性能索引
--
-- 仅用于重测"优化前"基线：
--   99_drop_perf_indexes.sql -> perf_probe.sh before -> 02_normalize_and_index.sql
--
-- 注意：uk_customer_clue_active 是函数唯一索引，回退后应用层的
--       数据库兜底防重会失效，重测完必须重新执行 02 恢复。
-- ============================================================
ALTER TABLE t_clue     DROP INDEX idx_clue_phone;
ALTER TABLE t_customer DROP INDEX idx_customer_deleted_clue;
ALTER TABLE t_customer DROP INDEX uk_customer_clue_active;
ALTER TABLE t_tran     DROP INDEX idx_tran_customer_stage;
ALTER TABLE t_activity DROP INDEX idx_activity_owner_create;

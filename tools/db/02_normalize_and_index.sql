-- ============================================================
-- 02 数据规范化 + 索引优化
--
-- 必须在 01_gen_perf_data.sql 之后执行：
--   01 造出 30 万级数据并记录"优化前"基线
--   02 再做规范化与索引优化，然后用 perf_probe.sh after 对比
--
-- 对应简历中的"慢查询治理"：EXPLAIN 定位全表扫描 -> 设计索引 -> 优化深分页
-- ============================================================

-- ------------------------------------------------------------
-- 1) 逻辑删除列规范化
--    deleted 可空时，查询不得不写成 "deleted = 0 OR deleted IS NULL"，
--    这个 OR 会让优化器放弃索引直接全表扫描。规范为 NOT NULL 后，
--    谓词可以简化为 deleted = 0，索引才用得上。
-- ------------------------------------------------------------
UPDATE t_clue     SET deleted = 0 WHERE deleted IS NULL;
UPDATE t_customer SET deleted = 0 WHERE deleted IS NULL;

ALTER TABLE t_clue
    MODIFY COLUMN deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除';

ALTER TABLE t_customer
    MODIFY COLUMN deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除';

-- ------------------------------------------------------------
-- 2) 清洗历史重复客户
--    同一线索保留最早创建的一条，其余置为已删除。
--    这一步同时说明：加锁只能防止"新增"重复，历史脏数据必须靠对账发现并清洗。
-- ------------------------------------------------------------
UPDATE t_customer c
JOIN (
    SELECT clue_id, MIN(id) AS keep_id
    FROM t_customer
    WHERE deleted = 0 AND clue_id IS NOT NULL
    GROUP BY clue_id
    HAVING COUNT(*) > 1
) d ON c.clue_id = d.clue_id
SET c.deleted = 1
WHERE c.deleted = 0 AND c.id <> d.keep_id;

-- ------------------------------------------------------------
-- 3) 数据库层兜底防重
--    应用层的分布式锁可能因超时/人工改库而失效，唯一索引是最后一道防线。
--    这里使用函数索引：仅对"未删除"的客户强制 clue_id 唯一，
--    已逻辑删除的记录不参与唯一性判断（否则软删后无法再次转换同一线索）。
-- ------------------------------------------------------------
ALTER TABLE t_customer
    ADD UNIQUE INDEX uk_customer_clue_active ((IF(deleted = 0, clue_id, NULL)));

-- ------------------------------------------------------------
-- 4) 列表与校验类查询索引
--
-- 以下索引全部经过 tools/db/perf_probe.sh 实测前后对比后确定，
-- 对比明细见 docs/perf/before-index.md 与 docs/perf/after-index.md。
-- ------------------------------------------------------------

-- 【实测收益 137x】新增线索前的手机号唯一性校验，优化前是全表扫描
ALTER TABLE t_clue
    ADD INDEX idx_clue_phone (phone);

-- 【实测收益 9.8x】客户按 deleted 过滤 + 按线索关联，对账与客户列表共用
ALTER TABLE t_customer
    ADD INDEX idx_customer_deleted_clue (deleted, clue_id);

-- 【按查询形态设计】交易列表支持按 customer_id + stage 过滤
ALTER TABLE t_tran
    ADD INDEX idx_tran_customer_stage (customer_id, stage);

-- 【按查询形态设计】活动列表支持按 owner_id + create_time 过滤；
--                   name 的前导通配 LIKE 无法走 B-tree，需另议（见报告 P7）
ALTER TABLE t_activity
    ADD INDEX idx_activity_owner_create (owner_id, create_time);

-- ------------------------------------------------------------
-- 5) 被否决的索引（保留结论，避免后人重复踩坑）
--
-- 曾尝试为线索列表加 idx_clue_deleted_id (deleted, id)，理由是
-- "同时满足 deleted 过滤与 id 排序，避免 filesort"。实测结果相反：
--
--   深分页 offset=300000：403ms -> 651ms（劣化 61%）
--
-- 原因是该查询需要返回 full_name 等非索引列：
--   - 全表扫描走聚簇索引，行数据就在扫描路径上，无额外开销；
--   - 走二级索引则每命中一行都要回表取 full_name，300 万次随机回表
--     远贵于一次顺序扫描。
--
-- 结论：深分页的根因是 OFFSET 本身，不是缺索引。解法见
--       TClueMapper 的游标分页（cursor 版实测 0.5ms）。
-- ------------------------------------------------------------

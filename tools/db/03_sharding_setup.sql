-- ============================================================
-- t_clue_remark 分表演练：按 clue_id MOD 4 建物理表并迁移存量
--
-- 用途：本地/真实环境把 t_clue_remark 切到分片数据源前的准备步骤。
-- CI 的集成测试会自建自清物理表，不依赖本脚本。
--
-- 关键设计：主键保持 int AUTO_INCREMENT，但 4 张物理表的起点错开 5 亿，
-- 保证跨片全局唯一（见 docs/sharding/README.md 的"为什么不用雪花 ID"）：
--   _0 起点   1        可用号段 [1,        5亿)
--   _1 起点 5亿        可用号段 [5亿,      10亿)
--   _2 起点 10亿       可用号段 [10亿,     15亿)
--   _3 起点 15亿       可用号段 [15亿,     20亿)  —— int 上限 21.47 亿
-- 每张表 5 亿行余量，对本项目"数十万级水位"是 1000 倍以上的余量。
-- ============================================================

-- 1. 建 4 张物理表（结构与原表一致，含外键）
CREATE TABLE IF NOT EXISTS `t_clue_remark_0` LIKE `t_clue_remark`;
CREATE TABLE IF NOT EXISTS `t_clue_remark_1` LIKE `t_clue_remark`;
CREATE TABLE IF NOT EXISTS `t_clue_remark_2` LIKE `t_clue_remark`;
CREATE TABLE IF NOT EXISTS `t_clue_remark_3` LIKE `t_clue_remark`;

-- 2. 错开各表自增起点（幂等：低于当前 max 时 MySQL 会忽略）
ALTER TABLE `t_clue_remark_0` AUTO_INCREMENT = 1;
ALTER TABLE `t_clue_remark_1` AUTO_INCREMENT = 500000000;
ALTER TABLE `t_clue_remark_2` AUTO_INCREMENT = 1000000000;
ALTER TABLE `t_clue_remark_3` AUTO_INCREMENT = 1500000000;

-- 3. 迁移存量数据（按与 sharding.yaml 相同的 clue_id MOD 4 路由）
INSERT INTO `t_clue_remark_0` SELECT * FROM `t_clue_remark` WHERE MOD(COALESCE(clue_id, 0), 4) = 0;
INSERT INTO `t_clue_remark_1` SELECT * FROM `t_clue_remark` WHERE MOD(COALESCE(clue_id, 0), 4) = 1;
INSERT INTO `t_clue_remark_2` SELECT * FROM `t_clue_remark` WHERE MOD(COALESCE(clue_id, 0), 4) = 2;
INSERT INTO `t_clue_remark_3` SELECT * FROM `t_clue_remark` WHERE MOD(COALESCE(clue_id, 0), 4) = 3;

-- 4. 校验迁移完整性（两侧行数应一致）
SELECT
  (SELECT COUNT(*) FROM `t_clue_remark`)                AS source_rows,
  (SELECT COUNT(*) FROM `t_clue_remark_0`) +
  (SELECT COUNT(*) FROM `t_clue_remark_1`) +
  (SELECT COUNT(*) FROM `t_clue_remark_2`) +
  (SELECT COUNT(*) FROM `t_clue_remark_3`)              AS sharded_rows;

-- 5. 确认无误后，把逻辑表重命名归档（分片数据源下应用不再直连它）：
-- RENAME TABLE `t_clue_remark` TO `t_clue_remark_backup`;
-- 回滚：改名回去 + DROP 四张物理表即可。

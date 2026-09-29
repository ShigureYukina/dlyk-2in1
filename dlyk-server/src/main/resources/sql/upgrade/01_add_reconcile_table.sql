-- ============================================================
-- 01 对账记录表
-- 用于承载定时对账任务发现的脏数据，供人工复核与后续修复
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_reconcile_record` (
  `id`          int          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type`    varchar(32)  NOT NULL COMMENT '对账类型：CLUE_CONVERT_MISSING / DUPLICATE_CUSTOMER / ORPHAN_CUSTOMER',
  `biz_key`     varchar(64)  NOT NULL COMMENT '业务键，如 clueId / customerId',
  `detail`      varchar(512) NULL DEFAULT NULL COMMENT '差异详情',
  `shard_index` int          NULL DEFAULT NULL COMMENT '执行分片序号',
  `shard_total` int          NULL DEFAULT NULL COMMENT '分片总数',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发现时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_biz_type_time`(`biz_type` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_biz_key`(`biz_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据一致性对账记录表';

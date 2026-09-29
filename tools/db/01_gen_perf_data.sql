-- ============================================================
-- 性能测试数据生成
--
-- 通过"自倍增"把 t_clue 快速推到十万级，再派生 t_customer / t_tran，
-- 用于复现"流水从万级涨到数十万级后列表联查变慢"的真实场景。
--
-- 用法（数据量可改最后一行 CALL 的参数）：
--   docker exec -i dlyk-mysql mysql -uroot -p*** dlyk < 01_gen_perf_data.sql
--
-- 注意：本脚本必须在索引优化（02_normalize_and_index.sql）之前执行，
--       否则无法复现优化前的慢查询基线。
-- ============================================================

SET SESSION innodb_lock_wait_timeout = 600;

DROP PROCEDURE IF EXISTS gen_perf_data;

DELIMITER $$
CREATE PROCEDURE gen_perf_data(IN target_clue INT)
BEGIN
    DECLARE cur INT DEFAULT 0;

    -- 1) 线索表自倍增到目标行数
    --    手机号重新随机，保证"按手机号查询"具备真实的选择性
    SELECT COUNT(*) INTO cur FROM t_clue;
    WHILE cur < target_clue DO
        INSERT INTO t_clue (owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job,
                            year_income, address, need_loan, intention_state, intention_product, state, source,
                            description, next_contact_time, create_time, create_by, edit_time, edit_by, deleted)
        SELECT owner_id,
               activity_id,
               full_name,
               appellation,
               CONCAT('13', LPAD(FLOOR(RAND() * 1000000000), 9, '0')),
               weixin, qq, email, age, job, year_income, address,
               need_loan, intention_state, intention_product, state, source, description,
               DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 720) HOUR),
               DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY),
               create_by, edit_time, edit_by, 0
        FROM t_clue;
        SELECT COUNT(*) INTO cur FROM t_clue;
    END WHILE;

    -- 2) 为尚无客户的线索补齐客户记录，保证 clue_id 唯一
    INSERT INTO t_customer (clue_id, product, description, next_contact_time,
                            create_time, create_by, edit_time, edit_by, deleted)
    SELECT c.id, 1, 'perf-data',
           DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 720) HOUR),
           DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY),
           1, NULL, NULL, 0
    FROM t_clue c
    LEFT JOIN t_customer cu ON cu.clue_id = c.id
    WHERE cu.id IS NULL;

    -- 3) 为客户派生交易流水
    INSERT INTO t_tran (tran_no, customer_id, money, expected_date, stage,
                        description, next_contact_time, create_time, create_by)
    SELECT CONCAT('TRAN', LPAD(cu.id, 10, '0')),
           cu.id,
           ROUND(1000 + RAND() * 90000, 2),
           DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 180) DAY),
           FLOOR(1 + RAND() * 5),
           'perf-data',
           DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 720) HOUR),
           DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY),
           1
    FROM t_customer cu
    LEFT JOIN t_tran t ON t.customer_id = cu.id
    WHERE t.id IS NULL;
END$$
DELIMITER ;

-- 目标线索量：30 万
CALL gen_perf_data(300000);
DROP PROCEDURE gen_perf_data;

SELECT 't_clue' AS tbl, COUNT(*) AS cnt FROM t_clue
UNION ALL SELECT 't_customer', COUNT(*) FROM t_customer
UNION ALL SELECT 't_tran', COUNT(*) FROM t_tran
UNION ALL SELECT 't_activity', COUNT(*) FROM t_activity;

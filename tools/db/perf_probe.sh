#!/usr/bin/env bash
# ============================================================
# 慢查询探针
#
# 索引优化前后各执行一次，输出可直接对比的 Markdown 报告：
#   tools/db/perf_probe.sh before > docs/perf/before-index.md
#   tools/db/perf_probe.sh after  > docs/perf/after-index.md
#
# 每项探针同时给出：
#   - EXPLAIN 的访问类型（type 列）：ALL 表示全表扫描，ref/range 表示走索引
#   - SQL 内 NOW(6) 计时得到的真实耗时，规避 mysql 客户端连接开销的干扰
# ============================================================
set -uo pipefail

LABEL="${1:-unknown}"
MYSQL="docker exec -i dlyk-mysql mysql -uroot -p1056398086 dlyk -N -B"

run_sql() {
  $MYSQL -e "$1" 2>/dev/null
}

# 真实列表联查的 FROM/WHERE 部分，与 TClueMapper.selectClueByPage 保持一致
LIST_FROM="FROM t_clue tc
  LEFT JOIN t_user tu1 ON tc.owner_id = tu1.id
  LEFT JOIN t_activity ta ON tc.activity_id = ta.id
  LEFT JOIN t_dic_value tdv  ON tc.appellation = tdv.id
  LEFT JOIN t_dic_value tdv2 ON tc.need_loan = tdv2.id
  LEFT JOIN t_dic_value tdv3 ON tc.intention_state = tdv3.id
  LEFT JOIN t_dic_value tdv4 ON tc.state = tdv4.id
  LEFT JOIN t_dic_value tdv5 ON tc.source = tdv5.id
  LEFT JOIN t_product tp ON tc.intention_product = tp.id"

echo "# 慢查询探针报告（${LABEL}）"
echo
echo "数据量："
run_sql "SELECT CONCAT('t_clue=', (SELECT COUNT(*) FROM t_clue),
                       ' t_customer=', (SELECT COUNT(*) FROM t_customer),
                       ' t_tran=', (SELECT COUNT(*) FROM t_tran));"
echo
echo "| 探针 | 说明 | EXPLAIN type | 耗时(ms) |"
echo "| --- | --- | --- | --- |"

probe() {
  local name="$1"; local desc="$2"; local sql="$3"

  local plan
  plan=$(run_sql "EXPLAIN $sql" | awk -F'\t' '{print $5}' | paste -sd, -)

  # 预热：先执行两次让数据页进入 buffer pool。
  # 否则建完索引立刻测，测到的是"冷启动"耗时，会把优化结果测成负收益。
  run_sql "$sql" > /dev/null
  run_sql "$sql" > /dev/null

  local ms
  ms=$(run_sql "SET @t0 := NOW(6); $sql; SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) / 1000, 1);" \
        | tail -n 1)

  printf '| %s | %s | %s | %s |\n' "$name" "$desc" "$plan" "$ms"
}

# P1 列表接口的存活过滤条件
probe "P1" "线索存活数（deleted 谓词）" \
  "SELECT COUNT(*) FROM t_clue WHERE deleted = 0"

# P2 新增线索前的手机号唯一性校验
probe "P2" "手机号唯一性校验 checkPhone" \
  "SELECT COUNT(0) FROM t_clue WHERE phone = '13900000001'"

# P3 真实列表联查 + 深分页（偏移 30 万后取 20 条）
probe "P3" "列表联查 深分页 offset=300000" \
  "SELECT tc.id, tc.full_name, tu1.name, ta.name, tdv4.type_value, tp.name $LIST_FROM
   WHERE tc.deleted = 0 ORDER BY tc.id DESC LIMIT 300000, 20"

# P4 真实列表联查 + 游标分页（从指定 id 之后取 20 条）
probe "P4" "列表联查 游标分页 id<56352" \
  "SELECT tc.id, tc.full_name, tu1.name, ta.name, tdv4.type_value, tp.name $LIST_FROM
   WHERE tc.deleted = 0 AND tc.id < 56352 ORDER BY tc.id DESC LIMIT 20"

# P5 对账：重复客户的线索
probe "P5" "对账-重复客户线索" \
  "SELECT clue_id FROM t_customer WHERE deleted = 0 GROUP BY clue_id HAVING COUNT(*) > 1"

# P6 交易列表联查
probe "P6" "交易列表与客户联查" \
  "SELECT COUNT(*) FROM t_tran t LEFT JOIN t_customer cu ON t.customer_id = cu.id WHERE cu.deleted = 0"

# P7 活动名称前导通配模糊搜索
probe "P7" "活动名称前导通配搜索" \
  "SELECT COUNT(*) FROM t_activity WHERE name LIKE '%活动%'"

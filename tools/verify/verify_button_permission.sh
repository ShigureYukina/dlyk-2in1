#!/usr/bin/env bash
# 验证 ClueController 的按钮级权限是否真的生效。
#
# ── 为什么要用 body 里的 code 判定，而不是 HTTP 状态码 ──────────────────────
# 这个项目所有业务响应都是 HTTP 200，错误放在 body 的 code 里：
#   未登录      → HTTP 200, {"code":901,"msg":"请求Token参数为空"}
#   权限不足    → HTTP 200, {"code":500,"msg":"权限不足"}
# TokenVerifyFilter 与 MyAccessDeniedHandler 都是直接 ResponseUtils.write()，
# 没有 setStatus，所以状态码一律是 200。
# 第一版脚本按 HTTP 状态码断言，于是「权限不足」被判成通过——
# 看起来像注解没生效，其实是我一直在看错误的信号。
#
# ── 根因背景 ──────────────────────────────────────────────────────────────
# ClueController 上的 @PreAuthorize 曾经长期被注释掉，根因是 TUser.getAuthorities()
# 读的是 permissionList（一个全仓库没有赋值路径的空壳字段），真正的权限在
# ButtonPermissionList。结果 authorities 里永远只有角色，任何 hasAnyAuthority('clue:*')
# 都匹配不上——注解一旦打开就全线 403。dlyk.sql 里还缺 clue:batchDelete /
# clue:convert / clue:export 三行权限。
#
# ── 验证方式 ──────────────────────────────────────────────────────────────
# 只用 admin 一个账号（dlyk.sql 里唯一密码已知且已验证可用的种子账号 admin/admin123），
# 通过临时摘掉 t_role_permission 的授权行制造「无权」状态，再恢复。
# 不去猜其他种子账号的密码——猜错会让这个脚本看起来像权限有问题。
#
# 关键：改完授权必须**重新登录**。TokenVerifyFilter 从 JWT 里解析用户并重建
# authorities，不回查数据库，所以权限变更要重新登录才生效。
#
# 前置：
#   1) MySQL 容器在跑（容器名默认 dlyk-mysql）
#   2) 应用已启动（默认 http://localhost:8089），Redis 可用
#   3) 已按 ../.env 导出 MYSQL_ROOT_PASSWORD、JWT_SECRET
#
# 用法（Git Bash / MSYS）：
#   export MSYS_NO_PATHCONV=1
#   cd crm/dlyk-2in1
#   set -a && . ./.env && set +a
#   bash tools/verify/verify_button_permission.sh

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8089}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-dlyk-mysql}"
DB="${DB:-dlyk}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PWD="${ADMIN_PWD:-admin123}"

pass=0
fail=0

ok()  { printf '  \033[32mPASS\033[0m %s\n' "$1"; pass=$((pass+1)); }
bad() { printf '  \033[31mFAIL\033[0m %s\n' "$1"; fail=$((fail+1)); }
info(){ printf '\n== %s ==\n' "$1"; }

if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
  echo "缺少 MYSQL_ROOT_PASSWORD。先执行：set -a && . ./.env && set +a" >&2
  exit 1
fi

sql() {
  docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$DB" -N -B -e "$1" 2>/dev/null
}

login() {
  curl -s -X POST "$BASE_URL/api/login" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode "loginAct=$ADMIN_USER" \
    --data-urlencode "loginPwd=$ADMIN_PWD"
}

fresh_token() {
  login | sed -n 's/.*"data":"\([^"]*\)".*/\1/p'
}

# body_code <token> <method> <path> —— 打印响应体里的业务 code
body_code() {
  local token="$1" method="$2" path="$3"
  curl -s -X "$method" "$BASE_URL$path" -H "Authorization: $token" \
    | sed -n 's/.*"code":\([0-9]*\).*/\1/p'
}

# 起始状态必须是「admin 持有 clue:list」，第 1 步才有意义。
# 第一版这里写的是 DELETE（想清理上次残留），结果把起始状态搞成了「无授权」，
# 第 1 步直接判失败——脚本自己和自己打架。正确做法是 INSERT IGNORE 保证它在。
sql "INSERT IGNORE INTO t_role_permission (role_id, permission_id) VALUES (1, 13)" >/dev/null

printf '准备：登录\n'
TOKEN="$(fresh_token)"
if [ -z "$TOKEN" ]; then
  echo "登录失败。请确认应用已启动、Redis 可用、dlyk.sql 已导入。" >&2
  exit 1
fi
echo "  token 已取得"

info "1) admin 持 clue:list 时应当放行（期望 code=200）"
code=$(body_code "$TOKEN" GET "/api/clues?current=1")
if [ "$code" = "200" ]; then ok "GET /api/clues → 200"
else bad "GET /api/clues → 期望 200，实际 code=$code"; fi

info "2) 摘掉 clue:list 后重新登录，应当被拦（期望 code=500 权限不足）"
sql "INSERT IGNORE INTO t_role_permission (role_id, permission_id) VALUES (1, 13)" >/dev/null
sql "DELETE FROM t_role_permission WHERE role_id=1 AND permission_id=13" >/dev/null
TOKEN="$(fresh_token)"
code=$(body_code "$TOKEN" GET "/api/clues?current=1")
if [ "$code" = "500" ]; then ok "GET /api/clues → 权限不足（@PreAuthorize 真的在拦）"
else bad "GET /api/clues → 期望 500，实际 code=$code"; fi

info "3) 同一 token 下，未被摘掉的权限应当仍然放行（证明不是一刀切拒绝）"
code=$(body_code "$TOKEN" GET "/api/clue/detail/1")
if [ "$code" = "200" ]; then ok "GET /api/clue/detail/1 → 200（clue:view 仍在，按端点精确生效）"
else bad "GET /api/clue/detail/1 → 期望 200，实际 code=$code（若也是 500，说明是被全局拒绝而不是按权限码判断）"; fi

info "4) 恢复授权后重新登录，应当重新放行（期望 code=200）"
sql "INSERT IGNORE INTO t_role_permission (role_id, permission_id) VALUES (1, 13)" >/dev/null
TOKEN="$(fresh_token)"
code=$(body_code "$TOKEN" GET "/api/clues?current=1")
if [ "$code" = "200" ]; then ok "GET /api/clues → 200（不是被永久拒掉的状态污染）"
else bad "GET /api/clues → 期望 200，实际 code=$code"; fi

info "5) 三条新补的权限行在库中存在"
for p in clue:batchDelete clue:convert clue:export; do
  n=$(sql "SELECT COUNT(*) FROM t_permission WHERE code='$p'")
  if [ "${n:-0}" -ge 1 ]; then ok "$p 有权限行"
  else bad "$p 缺权限行——@PreAuthorize 引用了库里不存在的 code，永远匹配不上"; fi
done

info "6) admin 是否持有全部线索域按钮权限"
n=$(sql "SELECT COUNT(*) FROM t_permission p
        JOIN t_role_permission rp ON rp.permission_id = p.id
        WHERE rp.role_id = 1 AND p.parent_id = 12 AND p.type = 'button'")
if [ "${n:-0}" -eq 9 ]; then ok "线索域按钮权限 9 个（list/add/edit/view/delete/import/batchDelete/convert/export）"
else bad "线索域按钮权限数 = $n，期望 9"; fi

printf '\n==== 结果：通过 %d，失败 %d ====\n' "$pass" "$fail"
[ "$fail" -eq 0 ] || exit 1

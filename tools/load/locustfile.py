"""
CRM 接口压测脚本（Locust）

用法：
    locust -f locustfile.py --host http://localhost:8089          # 带 Web UI
    locust -f locustfile.py --host http://localhost:8089 \
        --headless -u 50 -r 10 -t 60s --csv=result                 # 无人值守

环境变量：
    DLYK_LOAD_MODE   cached（默认）| bypass
                     bypass 模式会在访问热点接口前先清缓存，
                     用于测量"不走缓存"时的 DB 压力基线
    DLYK_DEEP_PAGE   深分页页码，默认 5000（约第 10 万条）
    DLYK_DIC_TYPES   逗号分隔的字典类型编码

bypass 模式除了清活动/字典缓存，还会清列表 total 缓存（见 ListTotalCache），
否则"无缓存"基线里仍带着这层缓存，会把优化效果算小。

任务权重按"前端真实访问配比"设置：字典与活动这类热点接口占比最高，
列表接口次之，深分页占比最低但最耗时。
"""

import os

from locust import HttpUser, between, task

MODE = os.getenv("DLYK_LOAD_MODE", "cached")
DIC_TYPES = os.getenv("DLYK_DIC_TYPES", "appellation,source,sex,needLoan").split(",")
DEEP_PAGE = int(os.getenv("DLYK_DEEP_PAGE", "5000"))

# 与 ListTotalCache.CACHE_NAME 对应，改了一边要改另一边
LIST_TOTAL_CACHE = "listTotalCache"


class CrmUser(HttpUser):
    """模拟 CRM 前端用户的访问行为"""

    wait_time = between(0.01, 0.05)

    def on_start(self):
        response = self.client.post(
            "/api/login",
            data={"loginAct": "admin", "loginPwd": "admin123"},
            name="POST /api/login",
        )
        self.token = response.json()["data"]
        self.dic_index = 0

    @property
    def auth(self):
        return {"Authorization": self.token}

    # ---------- 走缓存的热点接口 ----------

    @task(12)
    def cached_activities(self):
        """活动列表：@Cacheable(activityCache) 走 Redis 缓存"""
        self._maybe_bypass("activityCache")
        self.client.get("/api/activitys?current=1", headers=self.auth,
                        name="GET /api/activitys [缓存]")

    @task(10)
    def cached_dic_value(self):
        """字典值：Caffeine + Redis 双层缓存，前端每次渲染字典都会调用"""
        type_code = DIC_TYPES[self.dic_index % len(DIC_TYPES)]
        self.dic_index += 1
        self._maybe_bypass("dicTypeCache")
        self.client.get(f"/api/dicvalue/{type_code}", headers=self.auth,
                        name="GET /api/dicvalue/{typeCode} [缓存]")

    def _maybe_bypass(self, cache_name):
        """bypass 模式下先清空缓存，等价于"没有缓存"的对照实验"""
        if MODE == "bypass":
            self.client.delete(f"/api/cache/clear?cacheName={cache_name}",
                               headers=self.auth, name="DELETE /api/cache/clear")

    # ---------- 不走缓存的列表接口 ----------

    @task(8)
    def clue_list_offset_shallow(self):
        self._maybe_bypass(LIST_TOTAL_CACHE)
        self.client.get("/api/clues?current=1", headers=self.auth,
                        name="GET /api/clues?current=1 [偏移-浅页]")

    @task(5)
    def clue_list_offset_deep(self):
        """深分页：验证 OFFSET 越大越慢"""
        self._maybe_bypass(LIST_TOTAL_CACHE)
        self.client.get(f"/api/clues?current={DEEP_PAGE}", headers=self.auth,
                        name=f"GET /api/clues?current={DEEP_PAGE} [偏移-深页]")

    @task(8)
    def clue_list_cursor(self):
        """游标分页：深分页的对照实现"""
        self.client.get("/api/clues/cursor?size=20", headers=self.auth,
                        name="GET /api/clues/cursor [游标]")

    @task(5)
    def customer_list(self):
        self._maybe_bypass(LIST_TOTAL_CACHE)
        self.client.get("/api/customers?current=1", headers=self.auth,
                        name="GET /api/customers?current=1")

    @task(5)
    def check_phone(self):
        """新增线索前的手机号唯一性校验：全表扫描改走索引的那条查询"""
        self.client.get("/api/clue/13900000001", headers=self.auth,
                        name="GET /api/clue/{phone} [唯一性校验]")

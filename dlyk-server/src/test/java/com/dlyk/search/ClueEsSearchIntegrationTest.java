package com.dlyk.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.dlyk.model.TClue;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ES 检索链路端到端测试（真实 Elasticsearch）：
 * 全量重建 → 单条索引 → ngram 子串检索 / phone 前缀检索 → 删除文档。
 *
 * <p>本地没有 ES（未装 Docker）：靠 {@code DLYK_ES_IT=true} 门控，本地自动跳过；
 * CI 的 ci.yml 里起了 elasticsearch service 容器并注入该变量。
 * dlyk.mq.enabled=false 保持消费端不启动 —— 本测试直接驱动 ClueSearchService，
 * MQ 消费路径在单元测试与本地联调覆盖。
 */
@EnabledIfEnvironmentVariable(named = "DLYK_ES_IT", matches = "true")
@SpringBootTest(properties = {
        "dlyk.es.enabled=true",
        "dlyk.mq.enabled=false",
        // 同一 JVM 里的第二个上下文不能重复注册 XXL-Job 执行器（naming conflicts）
        "xxl.job.enabled=false",
        "management.server.port=0"
})
class ClueEsSearchIntegrationTest {

    @Autowired
    private ClueSearchService clueSearchService;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReindexThenSearchByFullNameSubstringAndPhonePrefix() throws Exception {
        long indexed = clueSearchService.reindexAll();
        Assertions.assertTrue(indexed > 0, "种子数据导入过 dlyk.sql，全量重建应索引到数据");

        String phone = "139" + (System.currentTimeMillis() % 100000000L) + "7";
        String name = "测式" + phone.substring(3, 7);
        jdbcTemplate.update(
                "insert into t_clue(full_name, phone, deleted, create_time) values (?, ?, 0, now())",
                name, phone);
        Integer id = jdbcTemplate.queryForObject(
                "select id from t_clue where phone = ?", Integer.class, phone);
        Assertions.assertNotNull(id);

        try {
            TClue clue = new TClue();
            clue.setId(id);
            clue.setFullName(name);
            clue.setPhone(phone);
            clue.setDeleted(0);
            clueSearchService.indexClue(clue);
            refreshIndex();

            // 姓名子串检索：ngram 保证连续子串能命中。库里可能有历史造数，只断言
            // "我们的文档在结果里"，总数用相对值断言删除前后的差
            PageInfo<TClue> byName = clueSearchService.search(1, name, null);
            Assertions.assertTrue(byName.getTotal() >= 1);
            Assertions.assertTrue(byName.getList().stream().anyMatch(c -> id.equals(c.getId())),
                    "姓名子串检索结果必须包含刚索引的线索");

            // 手机号前缀检索：用完整手机号做前缀 = 精确命中
            PageInfo<TClue> byPhone = clueSearchService.search(1, null, phone);
            Assertions.assertEquals(1, byPhone.getTotal());
            Assertions.assertEquals(phone, byPhone.getList().get(0).getPhone());

            // 删除文档后：总数减一，且我们的文档不再出现
            clueSearchService.deleteById(id);
            refreshIndex();
            PageInfo<TClue> afterDelete = clueSearchService.search(1, name, null);
            Assertions.assertEquals(byName.getTotal() - 1, afterDelete.getTotal());
            Assertions.assertFalse(afterDelete.getList().stream().anyMatch(c -> id.equals(c.getId())));
        } finally {
            jdbcTemplate.update("delete from t_clue where id = ?", id);
            clueSearchService.deleteById(id);
        }
    }

    private void refreshIndex() throws Exception {
        elasticsearchClient.indices().refresh(r -> r.index(ClueSearchDocument.INDEX));
    }
}

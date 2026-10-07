package com.dlyk.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.dlyk.constant.Constants;
import com.dlyk.mapper.TClueMapper;
import com.dlyk.model.TClue;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 线索 ES 检索：MySQL 为主库、ES 为检索副本。
 *
 * <p>职责切分刻意很窄 —— <b>ES 只负责命中与排序，详情由 MySQL 按 id 回表拼装</b>：
 * <ul>
 *   <li>列表页的 7 张关联表（归属人/活动/字典/产品）不冗余进索引，省掉一整类
 *       "字典改名了要不要刷索引"的一致性维护；</li>
 *   <li>命中后按 ES 的 id 顺序回 MySQL 查详情（复用 {@code selectCluesByIds} 的全 JOIN 查询），
 *       返回结构与原列表接口完全一致，前端无感。</li>
 * </ul>
 *
 * <p>分词用 ngram tokenizer（min 1 / max 10）而不是 ik：ngram 是 ES 内置能力，
 * 容器镜像无需装插件，CI 才能起真实 ES 跑集成测试；代价是索引体积变大、
 * 超过 max_gram 的连续子串搜不到。中文生产场景词库要求高时才值得上 ik。
 *
 * <p>一致性模型与对账同构：写入路径发 {@code clue.changed/deleted} 事件做实时增量
 * （发布失败只记日志，是旁路），索引缺口由管理端 {@link #reindexAll()} 全量重建兜底；
 * 查询侧 ES 异常时回落 MySQL 列表路径 —— 搜索是增强，不是依赖。
 */
@Service
public class ClueSearchService {

    private static final Logger log = LoggerFactory.getLogger(ClueSearchService.class);

    /** 全量重建时的游标扫描批大小 */
    private static final int REINDEX_BATCH_SIZE = 1000;

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private TClueMapper tClueMapper;

    @Value("${dlyk.es.enabled:false}")
    private boolean enabled;

    /**
     * 是否该走 ES 路径：开关打开且至少有一个关键字。
     * 纯翻页（无关键字）继续走 MySQL 缓存 + 游标分页，ES 只服务关键词检索。
     */
    public boolean shouldSearch(String name, String phone) {
        return enabled && (StringUtils.hasText(name) || StringUtils.hasText(phone));
    }

    /**
     * 关键词检索线索。fullName 走 ngram 子串匹配，phone 走 keyword 前缀匹配，
     * 按 id 倒序（与列表接口的排序语义一致）。
     *
     * <p>ES 不可用时异常直接抛给调用方：回落策略（放弃关键字走 MySQL 列表）
     * 由 {@code ClueServiceImpl} 统一实现 —— 搜索是增强不是依赖，但"静默返回空页"
     * 会让用户误以为没有结果，比一次可解释的回落更糟。
     */
    public PageInfo<TClue> search(Integer current, String name, String phone) {
        int cur = (current == null || current < 1) ? 1 : current;
        return doSearch(cur, name, phone);
    }

    private PageInfo<TClue> doSearch(int current, String name, String phone) {
        int pageSize = Constants.PAGE_SIZE;
        SearchResponse<ClueSearchDocument> response;
        try {
            response = elasticsearchClient.search(s -> s
                        .index(ClueSearchDocument.INDEX)
                        .from((current - 1) * pageSize)
                        .size(pageSize)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(q -> q.bool(b -> {
                            if (StringUtils.hasText(name)) {
                                b.must(m -> m.matchPhrase(mp -> mp.field("fullName").query(name.trim())));
                            }
                            if (StringUtils.hasText(phone)) {
                                b.must(m -> m.prefix(p -> p.field("phone").value(phone.trim())));
                            }
                            b.filter(f -> f.term(t -> t.field("deleted").value(0)));
                            return b;
                        }))
                        .sort(so -> so.field(f -> f.field("id").order(SortOrder.Desc))),
                    ClueSearchDocument.class);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("ES 检索请求失败", ex);
        }

        long total = response.hits().total() == null ? 0 : response.hits().total().value();
        List<Integer> ids = response.hits().hits().stream()
                .map(Hit::id)
                .filter(Objects::nonNull)
                .map(Integer::valueOf)
                .toList();
        if (ids.isEmpty()) {
            return buildPageInfo(List.of(), total, current);
        }
        // ES 只给 id 与顺序，详情回 MySQL 拼（保 ES 排序）
        List<TClue> rows = tClueMapper.selectCluesByIds(ids);
        return buildPageInfo(orderByIds(rows, ids), total, current);
    }

    /**
     * 按给定 id 顺序重排 MySQL 查回的行。包内可见，顺序逻辑单测直接覆盖。
     */
    static List<TClue> orderByIds(List<TClue> rows, List<Integer> ids) {
        Map<Integer, TClue> byId = rows.stream()
                .collect(Collectors.toMap(TClue::getId, Function.identity(), (a, b) -> a, HashMap::new));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private PageInfo<TClue> buildPageInfo(List<TClue> list, long total, int current) {
        PageInfo<TClue> pageInfo = new PageInfo<>(list);
        pageInfo.setTotal(total);
        pageInfo.setPageNum(current);
        pageInfo.setPages((int) ((total + Constants.PAGE_SIZE - 1) / Constants.PAGE_SIZE));
        return pageInfo;
    }

    /**
     * 把单条线索写进索引。发布/写入失败只记日志：缺口由 reindexAll 兜底。
     */
    public void indexClue(TClue clue) {
        if (!enabled || clue == null || clue.getId() == null) {
            return;
        }
        try {
            elasticsearchClient.index(i -> i.index(ClueSearchDocument.INDEX)
                    .id(String.valueOf(clue.getId()))
                    .document(toDocument(clue)));
        } catch (Exception ex) {
            log.warn("索引线索失败，等待全量重建兜底。id={}", clue.getId(), ex);
        }
    }

    public void deleteById(Integer clueId) {
        if (!enabled || clueId == null) {
            return;
        }
        try {
            elasticsearchClient.delete(d -> d.index(ClueSearchDocument.INDEX).id(String.valueOf(clueId)));
        } catch (Exception ex) {
            log.warn("删除线索文档失败，等待全量重建兜底。id={}", clueId, ex);
        }
    }

    /**
     * 全量重建：删掉旧索引重建映射，再按游标分页扫全表逐批索引。
     * 管理端入口 {@code POST /api/es/reindex}，也用于手工修复增量同步缺口。
     *
     * @return 索引的文档数
     */
    public long reindexAll() {
        if (!enabled) {
            throw new IllegalStateException("ES 检索未开启（dlyk.es.enabled=false），请先开启开关再重建索引");
        }
        recreateIndex();
        long count = 0;
        Integer lastId = null;
        while (true) {
            List<TClue> rows = tClueMapper.selectClueByCursor(lastId, REINDEX_BATCH_SIZE);
            if (rows.isEmpty()) {
                break;
            }
            BulkRequest.Builder bulk = new BulkRequest.Builder();
            for (TClue clue : rows) {
                bulk.operations(op -> op.index(idx -> idx
                        .index(ClueSearchDocument.INDEX)
                        .id(String.valueOf(clue.getId()))
                        .document(toDocument(clue))));
            }
            try {
                elasticsearchClient.bulk(bulk.build());
            } catch (java.io.IOException ex) {
                throw new IllegalStateException("ES 批量索引请求失败", ex);
            }
            count += rows.size();
            lastId = rows.get(rows.size() - 1).getId();
            if (rows.size() < REINDEX_BATCH_SIZE) {
                break;
            }
        }
        log.info("线索索引全量重建完成，共 {} 条", count);
        return count;
    }

    /**
     * 索引不存在则按 resources/elasticsearch/ 下的定义创建。
     * 由 {@link ClueIndexInitializer} 在启动期调用。
     */
    public void ensureIndex() {
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(e -> e.index(ClueSearchDocument.INDEX)).value();
            if (exists) {
                return;
            }
            recreateIndex();
            log.info("已创建线索搜索索引 {}", ClueSearchDocument.INDEX);
        } catch (Exception ex) {
            // ES 暂不可用不让应用起不来：检索开关打开后首次搜索会失败并回落，ES 恢复后重建即可
            log.warn("初始化线索搜索索引失败，应用继续启动：{}", ex.getMessage());
        }
    }

    private void recreateIndex() {
        co.elastic.clients.json.JsonpMapper mapper = elasticsearchClient._jsonpMapper();
        jakarta.json.spi.JsonProvider jsonp = mapper.jsonProvider();
        try (InputStream settings = getClass().getResourceAsStream("/elasticsearch/clue-settings.json");
             InputStream mappings = getClass().getResourceAsStream("/elasticsearch/clue-mappings.json");
             jakarta.json.stream.JsonParser settingsParser =
                     jsonp.createParser(new java.io.InputStreamReader(settings, java.nio.charset.StandardCharsets.UTF_8));
             jakarta.json.stream.JsonParser mappingsParser =
                     jsonp.createParser(new java.io.InputStreamReader(mappings, java.nio.charset.StandardCharsets.UTF_8))) {
            // 先删后建：已存在说明是重建场景；不存在时 delete 会 404，跳过即可
            if (elasticsearchClient.indices().exists(e -> e.index(ClueSearchDocument.INDEX)).value()) {
                elasticsearchClient.indices().delete(d -> d.index(ClueSearchDocument.INDEX));
            }
            elasticsearchClient.indices().create(c -> c.index(ClueSearchDocument.INDEX)
                    .settings(s -> s.withJson(settingsParser, mapper))
                    .mappings(m -> m.withJson(mappingsParser, mapper)));
        } catch (Exception ex) {
            throw new IllegalStateException("重建线索搜索索引失败", ex);
        }
    }

    private ClueSearchDocument toDocument(TClue clue) {
        return new ClueSearchDocument(
                clue.getId(),
                clue.getFullName(),
                clue.getPhone(),
                clue.getOwnerDO() == null ? null : clue.getOwnerDO().getName(),
                clue.getDeleted());
    }
}

package com.dlyk.web;

import com.dlyk.result.R;
import com.dlyk.search.ClueSearchService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ES 检索管理端点。
 *
 * <p>{@code POST /api/es/reindex}：全量重建线索索引。与"事件实时增量"构成
 * 事件驱动 + 周期兜底的同一套一致性叙事（对账思想的另一个应用）：
 * 增量同步丢消息、ES 集群故障恢复、映射变更等场景下，用一次全量重建把副本拉回与主库一致。
 *
 * @author ShigureYukina
 */
@RestController
public class EsAdminController {

    @Resource
    private ClueSearchService clueSearchService;

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/api/es/reindex")
    public R reindex() {
        long indexed = clueSearchService.reindexAll();
        return R.OK(Map.of("indexed", indexed));
    }
}

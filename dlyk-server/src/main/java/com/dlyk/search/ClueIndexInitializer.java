package com.dlyk.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动期确保线索搜索索引存在（含 ngram 分析器与映射定义）。
 *
 * <p>只在 {@code dlyk.es.enabled=true} 时注册 —— 开关关闭时整个检索链路
 * （初始化、消费、查询分支）都不工作，与 {@code dlyk.mq.enabled} 的开关语义一致。
 * ES 暂不可用只记 warn 不阻断启动：索引缺失时首次检索会失败并回落 MySQL，
 * ES 恢复后走一次 {@code POST /api/es/reindex} 即可补齐。
 */
@Component
@ConditionalOnProperty(name = "dlyk.es.enabled", havingValue = "true")
public class ClueIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClueIndexInitializer.class);

    private final ClueSearchService clueSearchService;

    public ClueIndexInitializer(ClueSearchService clueSearchService) {
        this.clueSearchService = clueSearchService;
    }

    @Override
    public void run(ApplicationArguments args) {
        clueSearchService.ensureIndex();
        log.info("线索搜索索引初始化完成");
    }
}

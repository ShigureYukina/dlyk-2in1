package com.dlyk.search;

import com.dlyk.model.TClue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

/**
 * {@link ClueSearchService} 纯逻辑单测：不依赖 ES。
 * 查询 DSL / 索引读写由 CI 的真实 ES 集成测试覆盖（ClueEsSearchIntegrationTest）。
 */
class ClueSearchServiceTest {

    private static TClue clue(int id, String name, String phone) {
        TClue tClue = new TClue();
        tClue.setId(id);
        tClue.setFullName(name);
        tClue.setPhone(phone);
        tClue.setDeleted(0);
        return tClue;
    }

    @Test
    void shouldReorderRowsByEsHitOrder() {
        List<TClue> rows = List.of(clue(1, "张三", "13800000001"), clue(2, "李四", "13800000002"), clue(3, "王五", "13800000003"));
        // ES 返回顺序是 id 倒序：3 在前 1 在后
        List<TClue> ordered = ClueSearchService.orderByIds(rows, List.of(3, 1));
        Assertions.assertEquals(List.of(3, 1), ordered.stream().map(TClue::getId).toList());
    }

    @Test
    void shouldSkipRowsMissingInMysql() {
        List<TClue> rows = List.of(clue(3, "王五", "13800000003"));
        // ES 与 MySQL 主库之间允许短暂不一致：库里已经查不到的 id 直接跳过，不能 NPE
        List<TClue> ordered = ClueSearchService.orderByIds(rows, List.of(999, 3));
        Assertions.assertEquals(List.of(3), ordered.stream().map(TClue::getId).toList());
    }

    @Test
    void shouldSearchOnlyWhenEnabledAndKeywordPresent() {
        ClueSearchService service = new ClueSearchService();
        ReflectionTestUtils.setField(service, "enabled", false);
        Assertions.assertFalse(service.shouldSearch("张三", null), "开关关闭时即使有关键字也不走 ES");
        Assertions.assertFalse(service.shouldSearch(null, "138"));

        ReflectionTestUtils.setField(service, "enabled", true);
        Assertions.assertTrue(service.shouldSearch(null, "138"), "只有手机号也应走 ES（前缀检索）");
        Assertions.assertTrue(service.shouldSearch("张三", null));
        Assertions.assertFalse(service.shouldSearch(" ", null), "纯空白不算关键字");
    }
}

package com.dlyk.search;

/**
 * 线索搜索文档（索引 dlyk-clue）。
 *
 * <p>刻意只放"检索 + 溯源"需要的最小字段：ES 只负责命中与排序，
 * 列表展示的 7 张关联表详情由 MySQL 按 id 拼装（见 {@link ClueSearchService#search}），
 * 避免把字典/归属人等关联数据冗余进索引后还要维护它们的一致性。
 * 只有 ownerName 冗余一份，供"按归属人过滤"这种高频维度直接在 ES 完成。
 *
 * @param id        线索 id，同时作为文档 _id
 * @param fullName  姓名，ngram 分词支持子串匹配
 * @param phone     手机号，keyword 前缀查询
 * @param ownerName 归属人姓名（冗余）
 * @param deleted   逻辑删除标记，查询侧恒等过滤 deleted=0
 */
public record ClueSearchDocument(Integer id, String fullName, String phone, String ownerName, Integer deleted) {

    public static final String INDEX = "dlyk-clue";
}

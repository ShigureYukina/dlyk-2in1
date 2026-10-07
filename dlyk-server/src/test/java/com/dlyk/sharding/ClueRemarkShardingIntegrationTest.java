package com.dlyk.sharding;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TClueRemarkMapper;
import com.dlyk.model.TClueRemark;
import com.dlyk.model.TUser;
import com.dlyk.query.ClueRemarkQuery;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;

/**
 * 分表链路端到端测试（真实 MySQL + ShardingSphere-JDBC 驱动模式）：
 * 建物理表 → 经 SS 按 clue_id MOD 4 路由插入 → 物理落位/全局唯一 id 断言
 * → 不带分片键的主键查询广播 → PageHelper 分页在 SS LIMIT 重写下可用。
 *
 * <p>物理表的 DDL 与落位断言走一条绕过 SS 的裸 JDBC 连接（直连逻辑库），
 * 保证验证的是"数据真的落在哪张物理表"，而不是 SS 自己的路由视图。
 * 测试自建自清物理表，CI 与本地都不依赖 03_sharding_setup.sql。
 *
 * <p>SS 驱动模式的 YAML 不会替换环境变量占位符，因此测试用
 * {@link DynamicPropertySource} 把模板渲染成临时文件再以 absolutepath 挂给驱动 ——
 * 与 tools/app/gen-sharding-config.sh 生成 + application-sharding.yml 消费是同一套机制。
 */
@SpringBootTest(properties = {
        // 同一 JVM 里的第二个上下文不能重复注册 XXL-Job 执行器（naming conflicts）
        "xxl.job.enabled=false",
        "dlyk.mq.enabled=false",
        "management.server.port=0"
})
class ClueRemarkShardingIntegrationTest {

    private static final int SHARD_COUNT = 4;

    @DynamicPropertySource
    static void shardingDatasource(DynamicPropertyRegistry registry) throws Exception {
        // SS 在启动期做单表/分片表元数据加载：物理表必须先于上下文创建，否则
        // 分片表的 actualDataNodes 元数据加载不到，运行期报 table does not exist
        try (Connection raw = DriverManager.getConnection(rawJdbcUrl(), "root", rawPassword())) {
            createPhysicalTables(raw);
        }
        Path yaml = Files.createTempFile("dlyk-sharding-", ".yaml");
        Files.writeString(yaml, renderTemplate(), StandardCharsets.UTF_8);
        String absolute = yaml.toAbsolutePath().toString().replace('\\', '/');
        registry.add("spring.datasource.url",
                () -> "jdbc:shardingsphere:absolutepath:" + absolute);
        registry.add("spring.datasource.driver-class-name",
                () -> "org.apache.shardingsphere.driver.ShardingSphereDriver");
    }

    /**
     * 渲染 sharding.template.yaml：占位符取环境变量（CI=SPRING_DATASOURCE_PASSWORD，
     * 本地=MYSQL_PASSWORD），与生成脚本 tools/app/gen-sharding-config.sh 同一套约定。
     */
    private static String renderTemplate() throws Exception {
        String template = new String(
                ClueRemarkShardingIntegrationTest.class.getResourceAsStream("/sharding.template.yaml")
                        .readAllBytes(), StandardCharsets.UTF_8);
        return template
                .replaceAll("\\$\\{SHARDING_DB_HOST:[^}]*}",
                        Matcher.quoteReplacement(env("MYSQL_HOST", "127.0.0.1")))
                .replaceAll("\\$\\{SHARDING_DB_PORT:[^}]*}",
                        Matcher.quoteReplacement(env("MYSQL_PORT", "3306")))
                .replaceAll("\\$\\{SHARDING_DB_USERNAME:[^}]*}",
                        Matcher.quoteReplacement(env("MYSQL_USERNAME", "root")))
                .replaceAll("\\$\\{SHARDING_DB_PASSWORD:[^}]*}",
                        Matcher.quoteReplacement(rawPassword()));
    }

    @Resource
    private TClueRemarkMapper tClueRemarkMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRouteByClueIdModAndKeepGlobalUniqueIds() throws Exception {
        try (Connection raw = DriverManager.getConnection(rawJdbcUrl(), "root", rawPassword())) {
            try {
                // 两条线索（外键目标），各自写两条跟进记录
                Long clueA = insertClue("分表测试线索A");
                Long clueB = insertClue("分表测试线索B");
                try {
                    TClueRemark a1 = insertRemark(clueA.intValue(), "A 的第一条跟进");
                    TClueRemark a2 = insertRemark(clueA.intValue(), "A 的第二条跟进");
                    TClueRemark b1 = insertRemark(clueB.intValue(), "B 的第一条跟进");

                    assertRoutedToPhysicalTable(raw, a1.getId(), clueA.intValue());
                    assertRoutedToPhysicalTable(raw, a2.getId(), clueA.intValue());
                    assertRoutedToPhysicalTable(raw, b1.getId(), clueB.intValue());
                    // a1/a2 同 clue_id → 落位断言等价于"同 clue 的记录进同一张物理表"

                    // 跨片全局唯一：四张物理表的 id 并集无重复
                    Assertions.assertEquals(countAll(raw), countDistinctIds(raw),
                            "物理表之间出现重复 id 说明分段 AUTO_INCREMENT 没生效");

                    // 不带分片键的主键查询：SS 广播 4 张表后归并
                    TClueRemark loaded = tClueRemarkMapper.selectByPrimaryKey(b1.getId());
                    Assertions.assertNotNull(loaded);
                    Assertions.assertEquals("B 的第一条跟进", loaded.getNoteContent());

                    // 带分片键的分页查询：PageHelper 的 LIMIT 改写在单片路由下可用
                    mockAdminRequestContext();
                    PageHelper.startPage(1, 1);
                    PageInfo<?> page = new PageInfo<>(tClueRemarkMapper.getActivityRemarkPage(
                            new ClueRemarkQuery() {{
                                setClueId(clueA.intValue());
                            }}));
                    Assertions.assertEquals(2, page.getTotal(), "线索 A 应有两条跟进记录");
                    Assertions.assertEquals(1, page.getList().size(), "每页 1 条");
                } finally {
                    jdbcTemplate.update("delete from t_clue where id in (?,?)", clueA, clueB);
                }
            } finally {
                dropPhysicalTables(raw);
            }
        }
    }

    // ----------------------------------------------------------- 断言辅助

    private void assertRoutedToPhysicalTable(Connection raw, Integer remarkId, long clueId) throws Exception {
        int expected = shardIndex(clueId);
        for (int i = 0; i < SHARD_COUNT; i++) {
            int found;
            try (Statement st = raw.createStatement();
                 ResultSet rs = st.executeQuery(
                         "select count(*) from t_clue_remark_" + i + " where id = " + remarkId)) {
                rs.next();
                found = rs.getInt(1);
            }
            if (i == expected) {
                Assertions.assertEquals(1, found, "id=" + remarkId + " 应落在 t_clue_remark_" + i);
            } else {
                Assertions.assertEquals(0, found, "id=" + remarkId + " 不应出现在 t_clue_remark_" + i);
            }
        }
    }

    private int shardIndex(long clueId) {
        return (int) (clueId % SHARD_COUNT);
    }

    private long countAll(Connection raw) throws Exception {
        long total = 0;
        for (int i = 0; i < SHARD_COUNT; i++) {
            try (Statement st = raw.createStatement();
                 ResultSet rs = st.executeQuery("select count(*) from t_clue_remark_" + i)) {
                rs.next();
                total += rs.getLong(1);
            }
        }
        return total;
    }

    private long countDistinctIds(Connection raw) throws Exception {
        StringBuilder union = new StringBuilder();
        for (int i = 0; i < SHARD_COUNT; i++) {
            if (!union.isEmpty()) {
                union.append(" union all ");
            }
            union.append("select id from t_clue_remark_").append(i);
        }
        try (Statement st = raw.createStatement();
             ResultSet rs = st.executeQuery("select count(distinct id) from (" + union + ") t")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    // ----------------------------------------------------------- 数据构造

    private Long insertClue(String fullName) {
        jdbcTemplate.update("insert into t_clue(full_name, deleted, create_time) values (?, 0, now())", fullName);
        return jdbcTemplate.queryForObject(
                "select id from t_clue where full_name = ? order by id desc limit 1", Long.class, fullName);
    }

    private TClueRemark insertRemark(Integer clueId, String content) {
        TClueRemark remark = new TClueRemark();
        remark.setClueId(clueId);
        remark.setNoteContent(content);
        remark.setCreateTime(new java.util.Date());
        int rows = tClueRemarkMapper.insertSelective(remark);
        Assertions.assertEquals(1, rows);
        Assertions.assertNotNull(remark.getId(), "useGeneratedKeys 应回填物理表生成的全局唯一 id");
        return remark;
    }

    /**
     * getActivityRemarkPage 带 @DataScope 切面：没有请求上下文会 NPE，
     * 伪造一个 admin 身份的请求（admin 不注入 filterSQL）。
     */
    private void mockAdminRequestContext() throws Exception {
        TUser admin = new TUser();
        admin.setId(1);
        admin.setName("it-admin");
        admin.setRoleList(java.util.List.of("admin"));
        String token = JWTUtils.createJWT(new ObjectMapper().writeValueAsString(admin));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Constants.TOKEN_HEADER, token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    // ----------------------------------------------------------- 物理表管理

    private static void createPhysicalTables(Connection raw) throws Exception {
        long[] bases = {1L, 500_000_000L, 1_000_000_000L, 1_500_000_000L};
        for (int i = 0; i < SHARD_COUNT; i++) {
            exec(raw, "DROP TABLE IF EXISTS t_clue_remark_" + i);
            exec(raw, "CREATE TABLE t_clue_remark_" + i + " LIKE t_clue_remark");
            exec(raw, "ALTER TABLE t_clue_remark_" + i + " AUTO_INCREMENT = " + bases[i]);
        }
    }

    private static void dropPhysicalTables(Connection raw) throws Exception {
        for (int i = 0; i < SHARD_COUNT; i++) {
            exec(raw, "DROP TABLE IF EXISTS t_clue_remark_" + i);
        }
    }

    private static void exec(Connection raw, String sql) throws Exception {
        try (Statement st = raw.createStatement()) {
            st.execute(sql);
        }
    }

    // ----------------------------------------------------------- 连接信息

    private static String rawJdbcUrl() {
        String host = env("MYSQL_HOST", "127.0.0.1");
        String port = env("MYSQL_PORT", "3306");
        return "jdbc:mysql://" + host + ":" + port + "/dlyk?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    }

    /**
     * 测试进程的密码来源与 application.yml 对齐：CI 用 SPRING_DATASOURCE_PASSWORD，
     * 本地习惯用 MYSQL_PASSWORD，最后兜底 root（CI service 容器值）。
     */
    private static String rawPassword() {
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (password == null || password.isEmpty()) {
            password = System.getenv("MYSQL_PASSWORD");
        }
        return password == null ? "root" : password;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }
}

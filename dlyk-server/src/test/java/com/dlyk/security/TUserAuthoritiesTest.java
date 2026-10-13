package com.dlyk.security;

import com.dlyk.model.TUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TUser#getAuthorities()} 的口径测试。
 *
 * <p>背景：{@code ClueController} 上的 {@code @PreAuthorize("hasAnyAuthority('clue:xxx')")}
 * 曾经长期被注释掉，根因不是「权限模型还没做完」，而是
 * {@code getAuthorities()} 读的是 {@code permissionList} —— 全仓库没有任何赋值路径，
 * 真正的权限落在 {@code ButtonPermissionList}（由 {@code UserServiceImpl.loadUserByUsername}
 * 填充并随 JWT 下发）。结果 authorities 里永远只有角色，
 * 任何 {@code hasAnyAuthority('clue:*')} 都匹配不上，注解一旦打开就全线 403。
 *
 * <p>这个测试锁住「权限码必须出现在 authorities 里」这条不变量，
 * 避免以后再有人把读字段改回 {@code permissionList}。
 *
 * <p>纯对象计算，不依赖 Spring 容器与数据库。
 */
class TUserAuthoritiesTest {

    private static Set<String> authorityNames(TUser user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("按钮权限码必须进入 authorities，而不只是角色")
    void buttonPermissionsAreExposedAsAuthorities() {
        TUser user = new TUser();
        user.setId(1);
        user.setRoleList(List.of("admin"));
        user.setButtonPermissionList(List.of("clue:list", "clue:convert"));

        Set<String> authorities = authorityNames(user);

        assertTrue(authorities.contains("admin"), "角色应在 authorities 里");
        assertTrue(authorities.contains("clue:list"), "clue:list 应在 authorities 里，否则 @PreAuthorize 必然 403");
        assertTrue(authorities.contains("clue:convert"), "clue:convert 应在 authorities 里");
    }

    @Test
    @DisplayName("角色与权限码都在时，两个来源都要保留")
    void rolesAndPermissionsAreBothPresent() {
        TUser user = new TUser();
        user.setRoleList(List.of("saler"));
        user.setButtonPermissionList(List.of("clue:list", "clue:add", "clue:edit"));

        Set<String> authorities = authorityNames(user);

        assertEquals(4, authorities.size(), "1 个角色 + 3 个权限码，不应互相覆盖");
        assertTrue(authorities.containsAll(Set.of("saler", "clue:list", "clue:add", "clue:edit")));
    }

    @Test
    @DisplayName("权限码缺失时不应回退去读 permissionList（那是历史空壳字段）")
    void doesNotFallBackToLegacyPermissionListField() {
        TUser user = new TUser();
        user.setRoleList(List.of("saler"));
        // 只设置历史遗留字段：getAuthorities() 不应该因此认为用户拥有 clue:delete
        user.setPermissionList(List.of("clue:delete"));

        Set<String> authorities = authorityNames(user);

        assertFalse(authorities.contains("clue:delete"),
                "permissionList 全仓库无赋值路径，若被读取会凭空造出权限；正确行为是忽略它");
    }

    @Test
    @DisplayName("角色与权限都为空时返回空集合，不抛 NPE")
    void emptyUserYieldsEmptyAuthorities() {
        TUser user = new TUser();

        assertTrue(authorityNames(user).isEmpty());
    }

    @Test
    @DisplayName("只有权限码没有角色时仍然可用（权限码本身就是 authority）")
    void permissionsWithoutRolesStillGrant() {
        TUser user = new TUser();
        user.setButtonPermissionList(List.of("clue:export"));

        assertEquals(Set.of("clue:export"), authorityNames(user));
    }
}

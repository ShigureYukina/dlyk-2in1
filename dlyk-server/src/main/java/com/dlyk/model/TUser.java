package com.dlyk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 用户表
 *
 * t_user
 */
@Data
public class TUser implements UserDetails, Serializable {

    /**
     * 主键，自动增长，用户ID
     */
    private Integer id;

    /**
     * 登录账号
     */
    private String loginAct;

    /**
     * 登录密码
     */
    private String loginPwd;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户手机
     */
    private String phone;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 账户是否没有过期，0已过期 1正常
     */
    private Integer accountNoExpired;

    /**
     * 密码是否没有过期，0已过期 1正常
     */
    private Integer credentialsNoExpired;

    /**
     * 账号是否没有锁定，0已锁定 1正常
     */
    private Integer accountNoLocked;

    /**
     * 账号是否启用，0禁用 1启用
     */
    private Integer accountEnabled;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private Integer createBy;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 编辑人
     */
    private Integer editBy;

    /**
     * 最近登录时间
     */
    private Date lastLoginTime;

    /**
     * 角色List
     */
    private List<String> roleList;

    /**
     * 权限标识符List
     *
     * @deprecated 历史遗留空壳字段：全仓库没有任何赋值路径，权限实际落在 {@link #ButtonPermissionList}。
     * 保留仅为兼容旧 JSON 结构，<b>不要</b>再往这里塞权限，否则 getAuthorities() 读不到。
     */
    @Deprecated
    private List<String> permissionList;
    /**
     * 权限标识符List
     */
    private List<TPermission> menuPermissionList;
    /**
     * 权限标识符List
     */
    private List<String> ButtonPermissionList;


    /**
     * 一对一关联
     */
    private TUser createByDO;
    private TUser editByDO;

    private static final long serialVersionUID = 1L;

    //-------------------------实现UserDetails接口的7个方法------------------------------

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        //角色
        if (!ObjectUtils.isEmpty(this.getRoleList())) {
            this.getRoleList().forEach(role -> {
                list.add(new SimpleGrantedAuthority(role));
            });
        }
        // 按钮级权限标识符（clue:list / clue:convert ...）。
        // 这里必须读 ButtonPermissionList —— 它才是 UserServiceImpl.loadUserByUsername 实际填充、
        // 并随 JWT 下发的那个字段；permissionList 是历史遗留的空壳字段，任何赋值路径都没有。
        // 原先读的是 permissionList，导致 getAuthorities() 永远只拿到角色，
        // Controller 上的 @PreAuthorize("hasAnyAuthority('clue:xxx')") 只要开着就必然 403。
        if (!ObjectUtils.isEmpty(this.getButtonPermissionList())) {
            this.getButtonPermissionList().forEach(permission -> {
                list.add(new SimpleGrantedAuthority(permission));
            });
        }
        return list;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return this.getLoginPwd();
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return this.getLoginAct();
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return this.getAccountNoExpired() == 1;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return this.getAccountNoLocked() == 1;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return this.getCredentialsNoExpired() == 1;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return this.getAccountEnabled() == 1;
    }
}
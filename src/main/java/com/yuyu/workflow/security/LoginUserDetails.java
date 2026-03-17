package com.yuyu.workflow.security;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security 登录用户上下文。
 */
@Getter
public class LoginUserDetails implements UserDetails {

    /**
     * 用户ID。
     */
    private final Long id;

    /**
     * 登录用户名。
     */
    private final String username;

    /**
     * 加密密码。
     */
    private final String password;

    /**
     * 真实姓名。
     */
    private final String realName;

    /**
     * 头像地址。
     */
    private final String avatar;

    /**
     * 状态值。
     */
    private final Integer status;

    /**
     * 角色编码集合。
     */
    private final List<String> roleCodes;

    /**
     * 权限标识集合。
     */
    private final List<String> permissions;

    /**
     * Spring Security 权限集合。
     */
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * 构造登录用户上下文。
     */
    public LoginUserDetails(Long id,
                            String username,
                            String password,
                            String realName,
                            String avatar,
                            Integer status,
                            List<String> roleCodes,
                            List<String> permissions,
                            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.avatar = avatar;
        this.status = status;
        this.roleCodes = roleCodes == null ? Collections.emptyList() : List.copyOf(roleCodes);
        this.permissions = permissions == null ? Collections.emptyList() : List.copyOf(permissions);
        this.authorities = authorities == null ? Collections.emptyList() : List.copyOf(authorities);
    }

    /**
     * 返回当前用户的授权集合。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * 返回当前用户的密码。
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 返回当前用户的登录名。
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 当前项目不启用账号过期控制。
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 当前项目不启用账号锁定控制。
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 当前项目不启用密码过期控制。
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 根据用户状态判断账号是否启用。
     */
    @Override
    public boolean isEnabled() {
        return CommonStatusEnum.ENABLED.getId().equals(status);
    }
}

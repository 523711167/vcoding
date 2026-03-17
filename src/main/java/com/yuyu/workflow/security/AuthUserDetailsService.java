package com.yuyu.workflow.security;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.mapper.SysMenuMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 基于数据库加载登录用户信息。
 */
@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    /**
     * 注入认证所需数据访问组件。
     */
    public AuthUserDetailsService(UserMapper userMapper,
                                  UserRoleMapper userRoleMapper,
                                  SysMenuMapper sysMenuMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.sysMenuMapper = sysMenuMapper;
    }

    /**
     * 按用户名加载登录用户信息。
     */
    @Override
    public LoginUserDetails loadUserByUsername(String username) {
        User user = userMapper.selectActiveByUsername(username);
        if (Objects.isNull(user)) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return buildLoginUser(user);
    }

    /**
     * 按用户ID加载登录用户信息。
     */
    public LoginUserDetails loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (Objects.isNull(user)) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), RespCodeEnum.UNAUTHORIZED.getMsg());
        }
        return buildLoginUser(user);
    }

    /**
     * 组装 Spring Security 所需的登录用户对象。
     */
    private LoginUserDetails buildLoginUser(User user) {
        if (!CommonStatusEnum.ENABLED.getId().equals(user.getStatus())) {
            throw new DisabledException("用户已停用");
        }
        List<String> roleCodes = userRoleMapper.selectEnabledCodesByUserId(user.getId());
        List<String> permissions = sysMenuMapper.selectEnabledPermissionsByUserId(user.getId());
        return new LoginUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getAvatar(),
                user.getStatus(),
                roleCodes,
                permissions,
                buildAuthorities(roleCodes, permissions)
        );
    }

    /**
     * 将角色编码和权限标识转换为 Spring Security 授权集合。
     */
    private List<GrantedAuthority> buildAuthorities(List<String> roleCodes, List<String> permissions) {
        Set<String> authoritySet = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(roleCodes)) {
            for (String roleCode : roleCodes) {
                if (StringUtils.hasText(roleCode)) {
                    authoritySet.add("ROLE_" + roleCode);
                }
            }
        }
        if (!CollectionUtils.isEmpty(permissions)) {
            for (String permission : permissions) {
                if (StringUtils.hasText(permission)) {
                    authoritySet.add(permission);
                }
            }
        }
        List<GrantedAuthority> result = new ArrayList<>();
        for (String authority : authoritySet) {
            result.add(new SimpleGrantedAuthority(authority));
        }
        return result;
    }
}

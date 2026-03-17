package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.convert.AuthStructMapper;
import com.yuyu.workflow.security.AuthUserDetailsService;
import com.yuyu.workflow.security.LoginUserDetails;
import com.yuyu.workflow.security.SecurityUtils;
import com.yuyu.workflow.service.AuthService;
import com.yuyu.workflow.vo.auth.AuthContextVO;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthUserDetailsService authUserDetailsService;
    private final AuthStructMapper authStructMapper;

    /**
     * 注入认证服务依赖组件。
     */
    public AuthServiceImpl(AuthUserDetailsService authUserDetailsService,
                           AuthStructMapper authStructMapper) {
        this.authUserDetailsService = authUserDetailsService;
        this.authStructMapper = authStructMapper;
    }

    /**
     * 获取当前登录用户信息。
     */
    @Override
    public AuthContextVO currentUser() {
        LoginUserDetails loginUser = authUserDetailsService.loadUserById(SecurityUtils.getCurrentUserId());
        return authStructMapper.toAuthContextVO(loginUser);
    }
}

package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuyu.workflow.config.SecurityTokenProperties;
import com.yuyu.workflow.convert.AuthStructMapper;
import com.yuyu.workflow.eto.auth.LoginETO;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.security.AuthUserDetailsService;
import com.yuyu.workflow.security.LoginUserDetails;
import com.yuyu.workflow.security.SecurityUtils;
import com.yuyu.workflow.security.TokenService;
import com.yuyu.workflow.service.AuthService;
import com.yuyu.workflow.vo.auth.AuthContextVO;
import com.yuyu.workflow.vo.auth.LoginVO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final SecurityTokenProperties securityTokenProperties;
    private final UserMapper userMapper;
    private final AuthUserDetailsService authUserDetailsService;
    private final AuthStructMapper authStructMapper;

    /**
     * 注入认证服务依赖组件。
     */
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           TokenService tokenService,
                           SecurityTokenProperties securityTokenProperties,
                           UserMapper userMapper,
                           AuthUserDetailsService authUserDetailsService,
                           AuthStructMapper authStructMapper) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.securityTokenProperties = securityTokenProperties;
        this.userMapper = userMapper;
        this.authUserDetailsService = authUserDetailsService;
        this.authStructMapper = authStructMapper;
    }

    /**
     * 校验用户名密码并签发 Token。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginETO eto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(eto.getUsername(), eto.getPassword())
        );
        LoginUserDetails loginUser = (LoginUserDetails) authentication.getPrincipal();
        updateLastLoginAt(loginUser.getId());
        String token = tokenService.createToken(loginUser);
        return authStructMapper.toLoginVO(token, securityTokenProperties.getTokenExpireSeconds(), loginUser);
    }

    /**
     * 获取当前登录用户信息。
     */
    @Override
    public AuthContextVO currentUser() {
        LoginUserDetails loginUser = authUserDetailsService.loadUserById(SecurityUtils.getCurrentUserId());
        return authStructMapper.toAuthContextVO(loginUser);
    }

    /**
     * 更新用户最后登录时间。
     */
    private void updateLastLoginAt(Long userId) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getLastLoginAt, LocalDateTime.now()));
    }
}

package com.yuyu.workflow.security;

import com.yuyu.workflow.security.password.PasswordLoginRequestMetadata;
import com.yuyu.workflow.service.LoginLogService;
import com.yuyu.workflow.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 登录成功事件监听器。
 *
 * <p>当认证服务端完成用户认证后，统一在这里维护最后登录时间，
 * 避免在控制器或自定义登录接口中重复处理。</p>
 */
@Component
public class AuthenticationSuccessEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationSuccessEventListener.class);
    private final UserService userService;
    private final LoginLogService loginLogService;

    /**
     * 注入登录成功处理依赖。
     */
    public AuthenticationSuccessEventListener(UserService userService,
                                              LoginLogService loginLogService) {
        this.userService = userService;
        this.loginLogService = loginLogService;
    }

    /**
     * 监听登录成功事件并更新用户最后登录时间。
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUserDetails loginUser)) {
            return;
        }
        LocalDateTime loginAt = LocalDateTime.now();
        userService.updateLastLoginAt(loginUser.getId(), loginAt);
        recordLoginSuccess(loginUser, event);
    }

    /**
     * 记录登录成功日志，不影响主认证流程。
     */
    private void recordLoginSuccess(LoginUserDetails loginUser, AuthenticationSuccessEvent event) {
        String clientIp = null;
        String userAgent = null;
        Object details = event.getAuthentication().getDetails();
        if (details instanceof PasswordLoginRequestMetadata metadata) {
            clientIp = metadata.getClientIp();
            userAgent = metadata.getUserAgent();
        }
        try {
            loginLogService.recordSuccess(loginUser.getId(), loginUser.getUsername(), clientIp, userAgent);
        } catch (Exception ex) {
            log.warn("记录登录成功日志失败, userId={}", loginUser.getId(), ex);
        }
    }
}

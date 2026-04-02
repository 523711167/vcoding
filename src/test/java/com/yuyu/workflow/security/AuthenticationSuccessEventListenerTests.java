package com.yuyu.workflow.security;

import com.yuyu.workflow.security.password.PasswordLoginRequestMetadata;
import com.yuyu.workflow.service.LoginLogService;
import com.yuyu.workflow.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 登录成功事件监听器测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationSuccessEventListenerTests {

    @Mock
    private UserService userService;

    @Mock
    private LoginLogService loginLogService;

    @InjectMocks
    private AuthenticationSuccessEventListener listener;

    /**
     * 登录成功时应更新最后登录时间并写入成功日志。
     */
    @Test
    void shouldRecordLoginSuccess() {
        LoginUserDetails loginUser = new LoginUserDetails(
                1L,
                "admin",
                "{noop}admin123",
                "系统管理员",
                "",
                1,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                loginUser,
                null,
                loginUser.getAuthorities()
        );
        authentication.setDetails(new PasswordLoginRequestMetadata("127.0.0.1", "JUnit-Agent"));
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        listener.onAuthenticationSuccess(event);

        ArgumentCaptor<LocalDateTime> loginTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userService).updateLastLoginAt(org.mockito.ArgumentMatchers.eq(1L), loginTimeCaptor.capture());
        assertNotNull(loginTimeCaptor.getValue());
        verify(loginLogService).recordSuccess(1L, "admin", "127.0.0.1", "JUnit-Agent");
    }

    /**
     * 非登录用户主体时应直接跳过。
     */
    @Test
    void shouldIgnoreWhenPrincipalIsNotLoginUser() {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                "client",
                null,
                Collections.emptyList()
        );
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        listener.onAuthenticationSuccess(event);

        verify(userService, never()).updateLastLoginAt(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        verify(loginLogService, never()).recordSuccess(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}

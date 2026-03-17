package com.yuyu.workflow.security;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.mapper.UserMapper;
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

    private final UserMapper userMapper;

    /**
     * 注入用户数据访问组件。
     */
    public AuthenticationSuccessEventListener(UserMapper userMapper) {
        this.userMapper = userMapper;
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
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, loginUser.getId())
                .set(User::getLastLoginAt, LocalDateTime.now()));
    }
}

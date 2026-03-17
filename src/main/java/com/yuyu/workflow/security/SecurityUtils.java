package com.yuyu.workflow.security;

import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类。
 */
public final class SecurityUtils {

    /**
     * 工具类不允许实例化。
     */
    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户上下文。
     */
    public static LoginUserDetails getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUserDetails loginUser)) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), RespCodeEnum.UNAUTHORIZED.getMsg());
        }
        return loginUser;
    }

    /**
     * 获取当前登录用户ID。
     */
    public static Long getCurrentUserId() {
        return getLoginUser().getId();
    }
}

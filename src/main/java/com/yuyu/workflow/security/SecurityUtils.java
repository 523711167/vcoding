package com.yuyu.workflow.security;

import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

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
    public static OAuth2AuthenticatedPrincipal getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal) {
            return principal;
        }
        throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), RespCodeEnum.UNAUTHORIZED.getMsg());
    }

    /**
     * 获取当前登录用户名。
     */
    public static String getCurrentUsername() {
        return getAuthenticatedPrincipal().getName();
    }

    /**
     * 获取当前登录用户ID。
     */
    public static Long getCurrentUserId() {
        Object value = getAuthenticatedPrincipal().getAttributes().get("user_id");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Long.parseLong(stringValue);
        }
        if (value == null) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), RespCodeEnum.UNAUTHORIZED.getMsg());
        }
        throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), RespCodeEnum.UNAUTHORIZED.getMsg());
    }
}

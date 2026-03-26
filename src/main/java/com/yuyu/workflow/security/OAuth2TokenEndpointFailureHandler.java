package com.yuyu.workflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 统一处理 OAuth2 token 端点认证失败响应。
 */
@Component
public class OAuth2TokenEndpointFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    /**
     * 注入 JSON 序列化组件。
     */
    public OAuth2TokenEndpointFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将认证失败结果转换为统一 JSON 响应。
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        Throwable target = unwrap(exception);
        Resp<Void> resp = buildResponse(target);
        response.setStatus(resolveHttpStatus(target));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), resp);
    }

    /**
     * 解开异常包装，尽量获取原始认证失败原因。
     */
    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * 构造统一响应体。
     */
    private Resp<Void> buildResponse(Throwable throwable) {
        if (throwable instanceof BadCredentialsException || throwable instanceof UsernameNotFoundException) {
            return Resp.fail(RespCodeEnum.UNAUTHORIZED.getId(), "用户名或密码错误");
        }
        if (throwable instanceof DisabledException) {
            return Resp.fail(RespCodeEnum.UNAUTHORIZED.getId(), "用户已停用");
        }
        if (throwable instanceof OAuth2AuthenticationException oauth2AuthenticationException) {
            OAuth2Error error = oauth2AuthenticationException.getError();
            if (OAuth2ErrorCodes.INVALID_REQUEST.equals(error.getErrorCode())) {
                return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(), defaultIfBlank(error.getDescription(), "请求参数错误"));
            }
            if (OAuth2ErrorCodes.INVALID_SCOPE.equals(error.getErrorCode())) {
                return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(), "scope无效");
            }
            if (OAuth2ErrorCodes.INVALID_CLIENT.equals(error.getErrorCode())
                    || OAuth2ErrorCodes.UNAUTHORIZED_CLIENT.equals(error.getErrorCode())
                    || OAuth2ErrorCodes.INVALID_GRANT.equals(error.getErrorCode())) {
                return Resp.fail(RespCodeEnum.UNAUTHORIZED.getId(), "用户名或密码错误");
            }
        }
        return Resp.fail(RespCodeEnum.UNAUTHORIZED.getId(), RespCodeEnum.UNAUTHORIZED.getName());
    }

    /**
     * 根据异常类型返回 HTTP 状态码。
     */
    private int resolveHttpStatus(Throwable throwable) {
        if (throwable instanceof OAuth2AuthenticationException oauth2AuthenticationException) {
            String errorCode = oauth2AuthenticationException.getError().getErrorCode();
            if (OAuth2ErrorCodes.INVALID_REQUEST.equals(errorCode) || OAuth2ErrorCodes.INVALID_SCOPE.equals(errorCode)) {
                return HttpServletResponse.SC_BAD_REQUEST;
            }
        }
        return HttpServletResponse.SC_UNAUTHORIZED;
    }

    /**
     * 返回非空文本。
     */
    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}

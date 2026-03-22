package com.yuyu.workflow.config;

import com.yuyu.workflow.common.base.UserContextParam;
import com.yuyu.workflow.security.SecurityUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * 统一填充请求体参数中的当前登录用户上下文。
 */
@ControllerAdvice
public class CurrentUserRequestBodyAdvice extends RequestBodyAdviceAdapter {

    /**
     * 仅处理继承用户上下文基类的请求体参数。
     */
    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasParameterAnnotation(RequestBody.class)
                && UserContextParam.class.isAssignableFrom(methodParameter.getParameterType());
    }

    /**
     * 请求体反序列化完成后补充当前操作人信息。
     */
    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (body instanceof UserContextParam userContextParam) {
            userContextParam.setCurrentUserId(SecurityUtils.getCurrentUserId());
            userContextParam.setCurrentUsername(SecurityUtils.getCurrentUsername());
        }
        return body;
    }
}

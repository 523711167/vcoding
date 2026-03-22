package com.yuyu.workflow.config;

import com.yuyu.workflow.common.base.UserContextParam;
import com.yuyu.workflow.security.SecurityUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

/**
 * 统一填充非请求体参数中的当前登录用户上下文。
 */
public class CurrentUserContextArgumentResolver implements HandlerMethodArgumentResolver {

    private final ServletModelAttributeMethodProcessor delegate;

    /**
     * 创建支持模型属性参数绑定的当前用户上下文解析器。
     */
    public CurrentUserContextArgumentResolver() {
        this.delegate = new ServletModelAttributeMethodProcessor(true);
    }

    /**
     * 处理所有继承用户上下文基类的非请求体参数。
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UserContextParam.class.isAssignableFrom(parameter.getParameterType())
                && !parameter.hasParameterAnnotation(RequestBody.class);
    }

    /**
     * 完成默认绑定后补充当前操作人信息。
     */
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        Object argument = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        if (argument instanceof UserContextParam userContextParam) {
            userContextParam.setCurrentUserId(SecurityUtils.getCurrentUserId());
            userContextParam.setCurrentUsername(SecurityUtils.getCurrentUsername());
        }
        return argument;
    }
}

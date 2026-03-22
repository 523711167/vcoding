package com.yuyu.workflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 扩展配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册统一的当前用户上下文参数解析器。
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserContextArgumentResolver());
    }
}

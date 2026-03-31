package com.yuyu.workflow.config;

import com.yuyu.workflow.common.context.OperationTimeContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 为每次 HTTP 请求统一初始化操作时间上下文。
 */
@Component
public class OperationTimeFilter extends OncePerRequestFilter {

    /**
     * 请求开始时写入统一操作时间，请求结束后清理上下文。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            OperationTimeContext.set(LocalDateTime.now());
            filterChain.doFilter(request, response);
        } finally {
            OperationTimeContext.clear();
        }
    }
}

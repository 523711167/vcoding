package com.yuyu.workflow.security;

import com.yuyu.workflow.common.exception.BizException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 基于 Bearer Token 的认证过滤器。
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final AuthUserDetailsService authUserDetailsService;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;

    /**
     * 注入 Token 认证所需依赖。
     */
    public TokenAuthenticationFilter(TokenService tokenService,
                                     AuthUserDetailsService authUserDetailsService,
                                     JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint) {
        this.tokenService = tokenService;
        this.authUserDetailsService = authUserDetailsService;
        this.jsonAuthenticationEntryPoint = jsonAuthenticationEntryPoint;
    }

    /**
     * 执行每次请求的 Token 鉴权逻辑。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = tokenService.resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            TokenClaims tokenClaims = tokenService.parseToken(token);
            LoginUserDetails loginUser = authUserDetailsService.loadUserById(tokenClaims.getUserId());
            if (!loginUser.getUsername().equals(tokenClaims.getUsername())) {
                throw new BizException("Token 用户信息不匹配");
            }
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (BizException ex) {
            handleInvalidToken(request, response, ex);
        }
    }

    /**
     * 对非法 Token 返回统一未登录响应。
     */
    private void handleInvalidToken(HttpServletRequest request,
                                    HttpServletResponse response,
                                    BizException ex) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, ex);
        jsonAuthenticationEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException(ex.getMessage(), ex)
        );
    }
}

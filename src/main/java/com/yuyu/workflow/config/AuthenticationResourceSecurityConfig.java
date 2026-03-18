package com.yuyu.workflow.config;

import com.yuyu.workflow.security.OAuth2AuthorizationServiceOpaqueTokenIntrospector;
import com.yuyu.workflow.security.JsonAccessDeniedHandler;
import com.yuyu.workflow.security.JsonAuthenticationEntryPoint;
import com.yuyu.workflow.security.AuthUserDetailsService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 资源服务端安全配置。
 */
@Configuration
@EnableMethodSecurity
public class AuthenticationResourceSecurityConfig {

    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    /**
     * 注入资源服务端异常处理组件。
     */
    public AuthenticationResourceSecurityConfig(JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
                                                JsonAccessDeniedHandler jsonAccessDeniedHandler) {
        this.jsonAuthenticationEntryPoint = jsonAuthenticationEntryPoint;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
    }

    /**
     * 配置业务资源接口过滤链。
     */
    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http,
                                                                OpaqueTokenIntrospector opaqueTokenIntrospector)
            throws Exception {
        http
                .securityMatcher("/sys/**", "/health/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/health/ping"
                        ).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(opaqueToken -> opaqueToken
                        .introspector(opaqueTokenIntrospector)));
        return http.build();
    }

    /**
     * 注册基于授权存储的 opaque token 内省器。
     */
    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector(OAuth2AuthorizationService authorizationService,
                                                           AuthUserDetailsService authUserDetailsService) {
        return new OAuth2AuthorizationServiceOpaqueTokenIntrospector(authorizationService, authUserDetailsService);
    }
}

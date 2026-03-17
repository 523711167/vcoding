package com.yuyu.workflow.config;

import com.yuyu.workflow.security.AuthUserDetailsService;
import com.yuyu.workflow.security.JsonAccessDeniedHandler;
import com.yuyu.workflow.security.JsonAuthenticationEntryPoint;
import com.yuyu.workflow.security.TokenAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityTokenProperties.class)
public class SecurityConfig {

    private final AuthUserDetailsService authUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    /**
     * 注入安全配置所需组件。
     */
    public SecurityConfig(AuthUserDetailsService authUserDetailsService,
                          PasswordEncoder passwordEncoder,
                          TokenAuthenticationFilter tokenAuthenticationFilter,
                          JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
                          JsonAccessDeniedHandler jsonAccessDeniedHandler) {
        this.authUserDetailsService = authUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.jsonAuthenticationEntryPoint = jsonAuthenticationEntryPoint;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
    }

    /**
     * 配置系统安全过滤链。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/health/ping",
                                "/sys/auth/login"
                        ).permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 注册基于数据库账户密码的认证提供者。
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(authUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    /**
     * 暴露认证管理器供登录接口使用。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}

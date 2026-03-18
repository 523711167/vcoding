package com.yuyu.workflow.config;

import com.yuyu.workflow.security.AuthUserDetailsService;
import com.yuyu.workflow.security.password.PasswordLoginGrantAuthenticationConverter;
import com.yuyu.workflow.security.password.PasswordLoginGrantAuthenticationProvider;
import com.yuyu.workflow.security.password.PasswordLoginGrantAuthenticationToken;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 认证服务端安全配置。
 *
 * <p>当前应用将认证服务端与资源服务端部署在同一个 Jar 内，
 * 但两者仍通过独立过滤链和 JWT 签发/校验职责进行隔离。</p>
 */
@Configuration
@EnableConfigurationProperties(SecurityTokenProperties.class)
public class AuthenticationServerSecurityConfig {

    private final SecurityTokenProperties securityTokenProperties;

    /**
     * 注入认证服务端配置属性。
     */
    public AuthenticationServerSecurityConfig(SecurityTokenProperties securityTokenProperties) {
        this.securityTokenProperties = securityTokenProperties;
    }

    /**
     * 配置 OAuth2 认证服务端端点过滤链。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            PasswordLoginGrantAuthenticationConverter passwordLoginGrantAuthenticationConverter,
            PasswordLoginGrantAuthenticationProvider passwordLoginGrantAuthenticationProvider) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .with(authorizationServerConfigurer, authorizationServer -> authorizationServer
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenRequestConverter(passwordLoginGrantAuthenticationConverter)
                                .authenticationProvider(passwordLoginGrantAuthenticationProvider)));
        return http.build();
    }

    /**
     * 注册当前应用内置的 OAuth2 客户端。
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(securityTokenProperties.getClientId())
                .clientSecret(passwordEncoder.encode(securityTokenProperties.getClientSecret()))
                .clientName(securityTokenProperties.getClientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scope("api")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofSeconds(securityTokenProperties.getTokenExpireSeconds()))
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    /**
     * 注册 OAuth2 授权信息存储实现。
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(DataSource dataSource,
                                                           RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(new JdbcTemplate(dataSource), registeredClientRepository);
    }

    /**
     * 注册 OAuth2 授权确认信息存储实现。
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(DataSource dataSource,
                                                                         RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(new JdbcTemplate(dataSource), registeredClientRepository);
    }

    /**
     * 注册认证服务端设置。
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(securityTokenProperties.getIssuer())
                .build();
    }

    /**
     * 注册访问令牌生成器。
     */
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(
            OAuth2TokenCustomizer<OAuth2TokenClaimsContext> opaqueTokenCustomizer) {
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        accessTokenGenerator.setAccessTokenCustomizer(opaqueTokenCustomizer);
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(accessTokenGenerator, refreshTokenGenerator);
    }

    /**
     * 注册自定义 password_login grant 请求转换器。
     */
    @Bean
    public PasswordLoginGrantAuthenticationConverter passwordLoginGrantAuthenticationConverter() {
        return new PasswordLoginGrantAuthenticationConverter();
    }

    /**
     * 注册自定义 password_login grant 认证提供者。
     */
    @Bean
    public PasswordLoginGrantAuthenticationProvider passwordLoginGrantAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            AuthenticationManager authenticationManager) {
        return new PasswordLoginGrantAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
    }

    /**
     * 自定义 opaque access token 声明，统一补充当前登录用户业务信息。
     */
    @Bean
    public OAuth2TokenCustomizer<OAuth2TokenClaimsContext> opaqueTokenCustomizer(
            AuthUserDetailsService authUserDetailsService) {
        return context -> {
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                return;
            }
            String username = resolveUsername(context.getAuthorization());
            if (!StringUtils.hasText(username)) {
                return;
            }
            var loginUser = authUserDetailsService.loadUserByUsername(username);
            context.getClaims().claim("user_id", String.valueOf(loginUser.getId()));
            context.getClaims().claim("real_name", loginUser.getRealName());
            context.getClaims().claim("status", String.valueOf(loginUser.getStatus()));
            context.getClaims().claim("roles", new ArrayList<>(loginUser.getRoleCodes()));
            context.getClaims().claim("permissions", new ArrayList<>(loginUser.getPermissions()));
        };
    }

    /**
     * 从授权上下文中提取当前资源拥有者用户名。
     */
    private String resolveUsername(OAuth2Authorization authorization) {
        if (authorization != null && StringUtils.hasText(authorization.getPrincipalName())) {
            return authorization.getPrincipalName();
        }
        return null;
    }
}

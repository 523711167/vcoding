package com.yuyu.workflow.security.password;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 自定义 password_login grant 认证提供者。
 */
public class PasswordLoginGrantAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final AuthenticationManager authenticationManager;

    /**
     * 注入自定义 grant 所需依赖。
     */
    public PasswordLoginGrantAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                                    OAuth2TokenGenerator<?> tokenGenerator,
                                                    AuthenticationManager authenticationManager) {
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.authenticationManager = authenticationManager;
    }

    /**
     * 处理 password_login grant 认证并签发令牌。
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        PasswordLoginGrantAuthenticationToken passwordLoginAuthentication =
                (PasswordLoginGrantAuthenticationToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClient(passwordLoginAuthentication.getPrincipal());
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
        if (!registeredClient.getAuthorizationGrantTypes()
                .contains(PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT));
        }
        if (!StringUtils.hasText(passwordLoginAuthentication.getUsername())
                || !StringUtils.hasText(passwordLoginAuthentication.getPassword())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "username或password不能为空", null));
        }

        Set<String> authorizedScopes = resolveAuthorizedScopes(passwordLoginAuthentication.getScopes(), registeredClient);
        Authentication userAuthentication;
        try {
            userAuthentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            passwordLoginAuthentication.getUsername(),
                            passwordLoginAuthentication.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "用户名或密码错误", null),
                    ex
            );
        }

        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE)
                .authorizationGrant(passwordLoginAuthentication);

        OAuth2AccessToken generatedAccessToken = generateAccessToken(tokenContextBuilder);
        OAuth2AccessToken accessToken = buildPersistedAccessToken(generatedAccessToken);
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(userAuthentication.getName())
                .authorizationGrantType(PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE)
                .authorizedScopes(authorizedScopes)
                .attribute(Principal.class.getName(), UsernamePasswordAuthenticationToken.authenticated(
                        userAuthentication.getName(),
                        null,
                        new ArrayList<>(userAuthentication.getAuthorities())
                ));
        attachAccessToken(authorizationBuilder, accessToken, generatedAccessToken);

        OAuth2RefreshToken refreshToken = generateRefreshToken(tokenContextBuilder, authorizationBuilder);
        if (refreshToken != null) {
            authorizationBuilder.refreshToken(refreshToken);
        }

        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);
        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientPrincipal,
                accessToken,
                refreshToken,
                Collections.emptyMap()
        );
    }

    /**
     * 判断当前认证提供者是否支持该认证类型。
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return PasswordLoginGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * 提取并校验已认证的客户端主体。
     */
    private OAuth2ClientAuthenticationToken getAuthenticatedClient(Object principal) {
        if (principal instanceof OAuth2ClientAuthenticationToken clientAuthenticationToken
                && clientAuthenticationToken.isAuthenticated()) {
            return clientAuthenticationToken;
        }
        throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT));
    }

    /**
     * 解析本次请求最终允许使用的 scope 集合。
     */
    private Set<String> resolveAuthorizedScopes(Set<String> requestedScopes, RegisteredClient registeredClient) {
        if (CollectionUtils.isEmpty(requestedScopes)) {
            return new LinkedHashSet<>(registeredClient.getScopes());
        }
        Set<String> authorizedScopes = new LinkedHashSet<>(requestedScopes);
        if (!registeredClient.getScopes().containsAll(authorizedScopes)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE));
        }
        return authorizedScopes;
    }

    /**
     * 生成 access token。
     */
    private OAuth2AccessToken generateAccessToken(DefaultOAuth2TokenContext.Builder tokenContextBuilder) {
        OAuth2AccessToken accessToken = (OAuth2AccessToken) tokenGenerator.generate(
                tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build()
        );
        if (accessToken == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR));
        }
        return accessToken;
    }

    /**
     * 生成 refresh token。
     */
    private OAuth2RefreshToken generateRefreshToken(DefaultOAuth2TokenContext.Builder tokenContextBuilder,
                                                    OAuth2Authorization.Builder authorizationBuilder) {
        OAuth2RefreshToken refreshToken = (OAuth2RefreshToken) tokenGenerator.generate(
                tokenContextBuilder
                        .authorization(authorizationBuilder.build())
                        .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                        .build()
        );
        return refreshToken;
    }

    /**
     * 将带有扩展 claims 的 access token 包装为标准 OAuth2AccessToken，
     * 避免 JDBC 按运行时子类类型存储后无法通过 getAccessToken() 读取。
     */
    private OAuth2AccessToken buildPersistedAccessToken(OAuth2AccessToken sourceAccessToken) {
        return new OAuth2AccessToken(
                sourceAccessToken.getTokenType(),
                sourceAccessToken.getTokenValue(),
                sourceAccessToken.getIssuedAt(),
                sourceAccessToken.getExpiresAt(),
                sourceAccessToken.getScopes()
        );
    }

    /**
     * 挂载 access token，并在存在 claims 时同步保存清洗后的元数据。
     */
    private void attachAccessToken(OAuth2Authorization.Builder authorizationBuilder,
                                   OAuth2AccessToken accessToken,
                                   OAuth2AccessToken sourceAccessToken) {
        authorizationBuilder.token(accessToken, metadata -> {
            if (sourceAccessToken instanceof ClaimAccessor claimAccessor) {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                        sanitizeValue(claimAccessor.getClaims()));
            }
        });
    }

    /**
     * 递归清洗 token 元数据中的集合实现，避免写入 JDBC 后触发 Jackson allowlist 反序列化异常。
     */
    private Object sanitizeValue(Object source) {
        if (source instanceof Map<?, ?> sourceMap) {
            Map<String, Object> sanitizedMap = new LinkedHashMap<>();
            sourceMap.forEach((key, value) -> sanitizedMap.put(String.valueOf(key), sanitizeValue(value)));
            return sanitizedMap;
        }
        if (source instanceof Iterable<?> iterable) {
            ArrayList<Object> sanitizedList = new ArrayList<>();
            for (Object item : iterable) {
                sanitizedList.add(sanitizeValue(item));
            }
            return sanitizedList;
        }
        if (source != null && source.getClass().isArray()) {
            int length = Array.getLength(source);
            ArrayList<Object> sanitizedList = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                sanitizedList.add(sanitizeValue(Array.get(source, index)));
            }
            return sanitizedList;
        }
        return source;
    }
}

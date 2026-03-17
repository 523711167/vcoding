package com.yuyu.workflow.security.password;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
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
import java.util.Collections;
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
        Authentication userAuthentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        passwordLoginAuthentication.getUsername(),
                        passwordLoginAuthentication.getPassword()
                )
        );

        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE)
                .authorizationGrant(passwordLoginAuthentication);

        OAuth2AccessToken accessToken = generateAccessToken(tokenContextBuilder);
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(userAuthentication.getName())
                .authorizationGrantType(PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE)
                .authorizedScopes(authorizedScopes)
                .attribute(Principal.class.getName(), userAuthentication);
        authorizationBuilder.token(accessToken, metadata -> {
            if (accessToken instanceof ClaimAccessor claimAccessor) {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claimAccessor.getClaims());
            }
        });

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
}

package com.yuyu.workflow.security.password;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 自定义 password_login grant 认证令牌。
 */
public class PasswordLoginGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    /**
     * 自定义 grant_type 常量。
     */
    public static final AuthorizationGrantType PASSWORD_LOGIN_GRANT_TYPE = new AuthorizationGrantType("password_login");

    /**
     * 登录用户名。
     */
    private final String username;

    /**
     * 登录密码。
     */
    private final String password;

    /**
     * 请求作用域集合。
     */
    private final Set<String> scopes;

    /**
     * 构造自定义 password_login grant 认证令牌。
     */
    public PasswordLoginGrantAuthenticationToken(Authentication clientPrincipal,
                                                 String username,
                                                 String password,
                                                 Set<String> scopes,
                                                 Map<String, Object> additionalParameters) {
        super(PASSWORD_LOGIN_GRANT_TYPE, clientPrincipal, additionalParameters);
        this.username = username;
        this.password = password;
        this.scopes = scopes == null ? Collections.emptySet() : Collections.unmodifiableSet(scopes);
    }

    /**
     * 获取登录用户名。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 获取登录密码。
     */
    public String getPassword() {
        return password;
    }

    /**
     * 获取请求作用域集合。
     */
    public Set<String> getScopes() {
        return scopes;
    }
}

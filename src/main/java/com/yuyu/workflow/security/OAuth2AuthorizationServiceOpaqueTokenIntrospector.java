package com.yuyu.workflow.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 OAuth2AuthorizationService 的 opaque token 内省器。
 *
 * <p>当前资源服务端与认证服务端部署在同一个 Jar 内，
 * 因此资源端可以直接读取授权存储判断 token 是否已撤销、是否过期，
 * 从而实现“撤销后立即失效”的效果。</p>
 */
public class OAuth2AuthorizationServiceOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final OAuth2AuthorizationService authorizationService;
    private final AuthUserDetailsService authUserDetailsService;

    /**
     * 注入授权存储服务。
     */
    public OAuth2AuthorizationServiceOpaqueTokenIntrospector(OAuth2AuthorizationService authorizationService,
                                                             AuthUserDetailsService authUserDetailsService) {
        this.authorizationService = authorizationService;
        this.authUserDetailsService = authUserDetailsService;
    }

    /**
     * 内省 access token 并转换为资源服务端可识别的主体对象。
     */
    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        OAuth2Authorization authorization;
        try {
            authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        } catch (DataRetrievalFailureException ex) {
            throw new BadOpaqueTokenException("Token已失效", ex);
        }
        if (authorization == null) {
            throw new BadOpaqueTokenException("Token不存在");
        }
        OAuth2Authorization.Token<?> accessToken = authorization.getAccessToken();
        if (accessToken == null || !accessToken.isActive()) {
            throw new BadOpaqueTokenException("Token已失效");
        }
        Map<String, Object> attributes = buildAttributes(authorization);
        return new DefaultOAuth2AuthenticatedPrincipal(
                authorization.getPrincipalName(),
                attributes,
                buildAuthorities(attributes)
        );
    }

    /**
     * 基于授权主体重新构造资源服务端所需属性，避免依赖 JDBC 序列化的复杂类型声明。
     */
    private Map<String, Object> buildAttributes(OAuth2Authorization authorization) {
        LoginUserDetails loginUser = authUserDetailsService.loadUserByUsername(authorization.getPrincipalName());
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("user_id", String.valueOf(loginUser.getId()));
        attributes.put("real_name", loginUser.getRealName());
        attributes.put("status", String.valueOf(loginUser.getStatus()));
        attributes.put("roles", new ArrayList<>(loginUser.getRoleCodes()));
        attributes.put("permissions", new ArrayList<>(loginUser.getPermissions()));
        attributes.put("scope", new ArrayList<>(authorization.getAuthorizedScopes()));
        return attributes;
    }

    /**
     * 从 token 声明中构造 Spring Security 权限集合。
     */
    private List<GrantedAuthority> buildAuthorities(Map<String, Object> attributes) {
        Set<GrantedAuthority> authoritySet = new LinkedHashSet<>();
        appendAuthorities(authoritySet, attributes.get("roles"), value -> "ROLE_" + value);
        appendAuthorities(authoritySet, attributes.get("permissions"), value -> value);
        appendAuthorities(authoritySet, attributes.get("scope"), value -> "SCOPE_" + value);
        return new ArrayList<>(authoritySet);
    }

    /**
     * 追加字符串集合类型的授权信息。
     */
    private void appendAuthorities(Set<GrantedAuthority> authoritySet,
                                   Object source,
                                   java.util.function.Function<String, String> authorityMapper) {
        if (!(source instanceof Iterable<?> iterable)) {
            return;
        }
        for (Object item : iterable) {
            if (item instanceof String value && StringUtils.hasText(value)) {
                authoritySet.add(new SimpleGrantedAuthority(authorityMapper.apply(value)));
            }
        }
    }
}

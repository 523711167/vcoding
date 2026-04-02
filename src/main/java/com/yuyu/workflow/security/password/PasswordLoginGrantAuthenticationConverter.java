package com.yuyu.workflow.security.password;

import com.yuyu.workflow.common.util.HttpRequestClientUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 自定义 password_login grant 请求转换器。
 */
public class PasswordLoginGrantAuthenticationConverter implements AuthenticationConverter {

    /**
     * 将 Token 端点请求转换为自定义 grant 认证令牌。
     */
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE.getValue().equals(grantType)) {
            return null;
        }
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        MultiValueMap<String, String> parameters = getParameters(request);
        String username = parameters.getFirst("username");
        String password = parameters.getFirst("password");
        String clientIp = HttpRequestClientUtils.resolveClientIp(request);
        String userAgent = HttpRequestClientUtils.resolveUserAgent(request);
        Set<String> scopes = parseScopes(parameters.getFirst(OAuth2ParameterNames.SCOPE));
        Map<String, Object> additionalParameters = new LinkedHashMap<>();
        parameters.forEach((key, values) -> {
            if (OAuth2ParameterNames.GRANT_TYPE.equals(key)
                    || OAuth2ParameterNames.SCOPE.equals(key)
                    || "username".equals(key)
                    || "password".equals(key)) {
                return;
            }
            if (!values.isEmpty()) {
                additionalParameters.put(key, values.get(0));
            }
        });
        return new PasswordLoginGrantAuthenticationToken(
                clientPrincipal,
                username,
                password,
                clientIp,
                userAgent,
                scopes,
                additionalParameters
        );
    }

    /**
     * 提取请求参数集合。
     */
    private MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> parameters.addAll(key, Arrays.asList(values)));
        return parameters;
    }

    /**
     * 解析 scope 字符串为集合。
     */
    private Set<String> parseScopes(String scope) {
        if (!StringUtils.hasText(scope)) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
    }
}

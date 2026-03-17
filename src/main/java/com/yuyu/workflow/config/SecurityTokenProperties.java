package com.yuyu.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证服务端与资源服务端共用安全配置属性。
 */
@Data
@ConfigurationProperties(prefix = "workflow.security")
public class SecurityTokenProperties {

    /**
     * 认证服务端签发 Token 时使用的 issuer。
     */
    private String issuer = "http://127.0.0.1:8080";

    /**
     * Token 有效时长，单位秒。
     */
    private Long tokenExpireSeconds = 7200L;

    /**
     * OAuth2 客户端标识。
     */
    private String clientId = "workflow-client";

    /**
     * OAuth2 客户端密钥。
     */
    private String clientSecret = "workflow-client-secret";

    /**
     * OAuth2 客户端名称。
     */
    private String clientName = "Workflow Client";

    /**
     * OAuth2 客户端回调地址。
     */
    private String clientRedirectUri = "http://127.0.0.1:8080/swagger-ui/oauth2-redirect.html";
}

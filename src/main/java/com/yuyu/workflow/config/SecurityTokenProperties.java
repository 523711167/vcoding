package com.yuyu.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token 认证配置属性。
 */
@Data
@ConfigurationProperties(prefix = "workflow.security")
public class SecurityTokenProperties {

    /**
     * Token 签名密钥。
     */
    private String tokenSecret;

    /**
     * Token 有效时长，单位秒。
     */
    private Long tokenExpireSeconds;
}

package com.yuyu.workflow.security.password;

/**
 * password_login 认证请求元数据。
 */
public class PasswordLoginRequestMetadata {

    private final String clientIp;
    private final String userAgent;

    /**
     * 构造认证请求元数据。
     */
    public PasswordLoginRequestMetadata(String clientIp, String userAgent) {
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    /**
     * 获取客户端 IP 地址。
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * 获取客户端 User-Agent。
     */
    public String getUserAgent() {
        return userAgent;
    }
}

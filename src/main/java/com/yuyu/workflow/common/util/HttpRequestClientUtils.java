package com.yuyu.workflow.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * HTTP 请求客户端信息解析工具。
 */
public final class HttpRequestClientUtils {

    private static final List<String> IP_HEADER_CANDIDATES = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_REAL_IP"
    );
    private static final String UNKNOWN = "unknown";
    private static final int CLIENT_IP_MAX_LENGTH = 64;
    private static final int USER_AGENT_MAX_LENGTH = 512;

    private HttpRequestClientUtils() {
    }

    /**
     * 解析客户端IP地址。
     */
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        for (String header : IP_HEADER_CANDIDATES) {
            String candidate = extractFirstIp(request.getHeader(header));
            if (StringUtils.hasText(candidate)) {
                return limit(candidate, CLIENT_IP_MAX_LENGTH);
            }
        }
        return limit(extractFirstIp(request.getRemoteAddr()), CLIENT_IP_MAX_LENGTH);
    }

    /**
     * 解析客户端 User-Agent。
     */
    public static String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userAgent = request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        return limit(userAgent.trim(), USER_AGENT_MAX_LENGTH);
    }

    /**
     * 从多值 IP 字符串中提取首个合法值。
     */
    private static String extractFirstIp(String ipValue) {
        if (!StringUtils.hasText(ipValue)) {
            return null;
        }
        String firstValue = ipValue.split(",")[0].trim();
        if (!StringUtils.hasText(firstValue) || UNKNOWN.equalsIgnoreCase(firstValue)) {
            return null;
        }
        return firstValue;
    }

    /**
     * 统一截断超长文本。
     */
    private static String limit(String source, int maxLength) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String trimmed = source.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}

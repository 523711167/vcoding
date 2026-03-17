package com.yuyu.workflow.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.config.SecurityTokenProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Token 生成与解析服务。
 */
@Component
public class TokenService {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final SecurityTokenProperties securityTokenProperties;

    /**
     * 注入 Token 相关依赖。
     */
    public TokenService(ObjectMapper objectMapper, SecurityTokenProperties securityTokenProperties) {
        this.objectMapper = objectMapper;
        this.securityTokenProperties = securityTokenProperties;
    }

    /**
     * 为当前登录用户生成 Token。
     */
    public String createToken(LoginUserDetails loginUser) {
        long expireAt = Instant.now().getEpochSecond() + securityTokenProperties.getTokenExpireSeconds();
        Map<String, Object> headerMap = new LinkedHashMap<>();
        headerMap.put("alg", "HS256");
        headerMap.put("typ", "JWT");
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("userId", loginUser.getId());
        payloadMap.put("username", loginUser.getUsername());
        payloadMap.put("exp", expireAt);
        String header = encodeJson(headerMap);
        String payload = encodeJson(payloadMap);
        String content = header + "." + payload;
        return content + "." + sign(content);
    }

    /**
     * 解析并校验 Token。
     */
    public TokenClaims parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), RespCodeEnum.UNAUTHORIZED.getMsg());
        }
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length != 3) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), "Token 格式错误");
        }
        String content = tokenParts[0] + "." + tokenParts[1];
        String expectedSignature = sign(content);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                tokenParts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), "Token 签名无效");
        }
        Map<String, Object> payloadMap = decodeJson(tokenParts[1]);
        TokenClaims tokenClaims = buildTokenClaims(payloadMap);
        if (tokenClaims.getExpireAt() <= Instant.now().getEpochSecond()) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), "Token 已过期");
        }
        return tokenClaims;
    }

    /**
     * 从请求头中提取 Bearer Token。
     */
    public String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        return authorization.substring(TOKEN_PREFIX.length());
    }

    /**
     * 将对象编码为 Base64Url JSON 片段。
     */
    private String encodeJson(Map<String, Object> data) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(data));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Token JSON encode failed", ex);
        }
    }

    /**
     * 将 Base64Url JSON 片段反序列化为 Map。
     */
    private Map<String, Object> decodeJson(String encodedValue) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedValue);
            return objectMapper.readValue(decodedBytes, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), "Token 解析失败");
        }
    }

    /**
     * 将声明 Map 转换为业务 Token 声明对象。
     */
    private TokenClaims buildTokenClaims(Map<String, Object> payloadMap) {
        Long userId = castToLong(payloadMap.get("userId"));
        Long expireAt = castToLong(payloadMap.get("exp"));
        Object usernameValue = payloadMap.get("username");
        if (Objects.isNull(userId) || Objects.isNull(expireAt) || !(usernameValue instanceof String username)
                || !StringUtils.hasText(username)) {
            throw new BizException(RespCodeEnum.UNAUTHORIZED.getId(), "Token 声明缺失");
        }
        TokenClaims tokenClaims = new TokenClaims();
        tokenClaims.setUserId(userId);
        tokenClaims.setUsername(username);
        tokenClaims.setExpireAt(expireAt);
        return tokenClaims;
    }

    /**
     * 将对象安全转换为 Long。
     */
    private Long castToLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * 使用 HMAC-SHA256 对内容进行签名。
     */
    private String sign(String content) {
        if (!StringUtils.hasText(securityTokenProperties.getTokenSecret())) {
            throw new IllegalStateException("workflow.security.token-secret must not be blank");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(securityTokenProperties.getTokenSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Token sign failed", ex);
        }
    }
}

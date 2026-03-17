package com.yuyu.workflow.security;

import lombok.Data;

/**
 * Token 解析后的声明对象。
 */
@Data
public class TokenClaims {

    /**
     * 当前登录用户ID。
     */
    private Long userId;

    /**
     * 当前登录用户名。
     */
    private String username;

    /**
     * Token 过期时间戳，单位秒。
     */
    private Long expireAt;
}

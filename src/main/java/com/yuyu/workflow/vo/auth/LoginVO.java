package com.yuyu.workflow.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录返回对象。
 */
@Data
@Schema(description = "登录返回对象")
public class LoginVO {

    /**
     * Bearer Token。
     */
    @Schema(description = "访问 Token", example = "eyJhbGciOiJIUzI1NiJ9.xxx.yyy")
    private String token;

    /**
     * Token 类型。
     */
    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType;

    /**
     * 过期时长，单位秒。
     */
    @Schema(description = "过期时长，单位秒", example = "7200")
    private Long expireSeconds;

    /**
     * 当前登录用户上下文。
     */
    @Schema(description = "当前登录用户上下文")
    private AuthContextVO user;
}

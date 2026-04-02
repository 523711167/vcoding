package com.yuyu.workflow.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志返回对象。
 */
@Data
@Schema(description = "登录日志返回对象")
public class LoginLogVO {

    @Schema(description = "登录日志ID")
    private Long id;

    @Schema(description = "登录用户ID")
    private Long userId;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "登录结果：SUCCESS/FAIL")
    private String result;

    @Schema(description = "登录结果说明")
    private String resultMsg;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "客户端IP地址")
    private String clientIp;

    @Schema(description = "客户端User-Agent")
    private String userAgent;

    @Schema(description = "登录发生时间")
    private LocalDateTime loginAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}

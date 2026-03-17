package com.yuyu.workflow.vo.auth;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户上下文。
 */
@Data
@Schema(description = "当前登录用户上下文")
public class AuthContextVO {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 登录用户名。
     */
    @Schema(description = "登录用户名", example = "admin")
    private String username;

    /**
     * 真实姓名。
     */
    @Schema(description = "真实姓名", example = "系统管理员")
    private String realName;

    /**
     * 头像地址。
     */
    @Schema(description = "头像地址", example = "https://example.com/avatar.png")
    private String avatar;

    /**
     * 状态值。
     */
    @Schema(description = "状态值", example = "1")
    private Integer status;

    /**
     * 状态文案。
     */
    @Schema(description = "状态文案", example = "正常")
    private String statusMsg;

    /**
     * 角色编码集合。
     */
    @ArraySchema(schema = @Schema(description = "角色编码", example = "ADMIN"))
    private List<String> roleCodes;

    /**
     * 权限标识集合。
     */
    @ArraySchema(schema = @Schema(description = "权限标识", example = "sys:user:list"))
    private List<String> permissions;
}

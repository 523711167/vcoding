package com.yuyu.workflow.eto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
@Schema(description = "登录请求参数")
public class LoginETO {

    /**
     * 登录用户名。
     */
    @NotBlank(message = "username不能为空")
    @Schema(description = "登录用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    /**
     * 登录密码。
     */
    @NotBlank(message = "password不能为空")
    @Schema(description = "登录密码", example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}

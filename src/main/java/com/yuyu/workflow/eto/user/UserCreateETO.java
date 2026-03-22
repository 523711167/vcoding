package com.yuyu.workflow.eto.user;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "新增用户参数")
public class UserCreateETO extends UserContextParam {

    @Schema(description = "登录用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "username不能为空")
    @Size(max = 64, message = "username长度不能超过64")
    private String username;

    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "password不能为空")
    @Size(max = 64, message = "password长度不能超过64")
    private String password;

    @Schema(description = "真实姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "realName不能为空")
    @Size(max = 64, message = "realName长度不能超过64")
    private String realName;

    @Schema(description = "邮箱")
    @Size(max = 128, message = "email长度不能超过128")
    private String email;

    @Schema(description = "手机号")
    @Size(max = 20, message = "mobile长度不能超过20")
    private String mobile;

    @Schema(description = "头像地址")
    @Size(max = 256, message = "avatar长度不能超过256")
    private String avatar;
}

package com.yuyu.workflow.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "角色或组织视角下的用户简要信息")
public class UserSimpleVO {

    @Schema(description = "用户ID")
    private Long id;
    @Schema(description = "登录用户名")
    private String username;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "手机号")
    private String mobile;
    @Schema(description = "状态")
    private Integer status;
}

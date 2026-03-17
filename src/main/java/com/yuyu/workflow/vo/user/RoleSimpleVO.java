package com.yuyu.workflow.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户视角下的角色简要信息")
public class RoleSimpleVO {

    @Schema(description = "角色ID")
    private Long id;
    @Schema(description = "角色名称")
    private String name;
    @Schema(description = "角色编码")
    private String code;
    @Schema(description = "角色状态")
    private Integer status;
}

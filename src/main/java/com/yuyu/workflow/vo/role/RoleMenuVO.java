package com.yuyu.workflow.vo.role;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色菜单授权返回对象。
 */
@Data
@Schema(description = "角色菜单授权返回对象")
public class RoleMenuVO {

    @Schema(description = "角色ID")
    private Long roleId;

    @ArraySchema(schema = @Schema(description = "菜单ID"))
    private List<Long> menuIds;
}

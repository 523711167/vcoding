package com.yuyu.workflow.eto.role;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 全量更新角色菜单参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "全量更新角色菜单参数")
public class RoleMenusUpdateETO extends UserContextParam {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "roleId不能为空")
    private Long roleId;

    @ArraySchema(schema = @Schema(description = "菜单ID"))
    private List<Long> menuIds;
}

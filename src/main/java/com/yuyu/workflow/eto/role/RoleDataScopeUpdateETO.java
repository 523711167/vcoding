package com.yuyu.workflow.eto.role;

import com.yuyu.workflow.common.enums.DataScopeEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新角色数据权限参数")
public class RoleDataScopeUpdateETO {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "roleId不能为空")
    private Long roleId;

    @Schema(description = "数据权限范围", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "dataScope不能为空")
    @EnumIdValid(enumClass = DataScopeEnum.class, allowNull = false, message = "dataScope不合法")
    private Integer dataScope;

    @ArraySchema(schema = @Schema(description = "自定义组织ID"))
    private List<Long> deptIds;
}

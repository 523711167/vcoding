package com.yuyu.workflow.eto.role;

import com.yuyu.workflow.common.base.UserContextParam;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "更新角色状态参数")
public class RoleStatusUpdateETO extends UserContextParam {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "roleId不能为空")
    private Long roleId;

    @Schema(description = "状态：1=正常 0=停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "status不能为空")
    @EnumIdValid(enumClass = CommonStatusEnum.class, allowNull = false, message = "status不合法")
    private Integer status;
}

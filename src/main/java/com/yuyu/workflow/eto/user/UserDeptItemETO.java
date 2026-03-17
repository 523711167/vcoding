package com.yuyu.workflow.eto.user;

import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "用户组织项参数")
public class UserDeptItemETO {

    @Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "deptId不能为空")
    private Long deptId;

    @Schema(description = "是否主组织：1=是 0=否", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "isPrimary不能为空")
    @EnumIdValid(enumClass = YesNoEnum.class, allowNull = false, message = "isPrimary不合法")
    private Integer isPrimary;
}

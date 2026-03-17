package com.yuyu.workflow.eto.user;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新用户状态参数")
public class UserStatusUpdateETO {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "状态：1=正常 0=停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "status不能为空")
    @EnumIdValid(enumClass = CommonStatusEnum.class, allowNull = false, message = "status不合法")
    private Integer status;
}

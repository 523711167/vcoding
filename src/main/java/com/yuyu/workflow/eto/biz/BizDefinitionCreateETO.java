package com.yuyu.workflow.eto.biz;

import com.yuyu.workflow.common.base.UserContextParam;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 新增业务定义参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "新增业务定义参数")
public class BizDefinitionCreateETO extends UserContextParam {

    @Schema(description = "业务编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "bizCode不能为空")
    @Size(max = 64, message = "bizCode长度不能超过64")
    private String bizCode;

    @Schema(description = "业务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "bizName不能为空")
    @Size(max = 128, message = "bizName长度不能超过128")
    private String bizName;

    @Schema(description = "业务描述")
    @Size(max = 500, message = "bizDesc长度不能超过500")
    private String bizDesc;

    @Schema(description = "绑定流程定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "workflowDefinitionId不能为空")
    private Long workflowDefinitionId;

    @Schema(description = "状态：1=正常 0=停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "status不能为空")
    @EnumIdValid(enumClass = CommonStatusEnum.class, allowNull = false, message = "status不合法")
    private Integer status;
}

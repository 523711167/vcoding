package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改流程定义参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "修改流程定义参数")
public class WorkflowDefinitionUpdateETO extends UserContextParam {

    @Schema(description = "流程定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "流程名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name不能为空")
    @Size(max = 100, message = "name长度不能超过100")
    private String name;

    @Schema(description = "流程描述")
    @Size(max = 500, message = "description长度不能超过500")
    private String description;

    @Schema(description = "流程JSON字符串", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "workFlowJson不能为空")
    private String workFlowJson;
}

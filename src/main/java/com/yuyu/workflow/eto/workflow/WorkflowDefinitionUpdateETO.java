package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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

    @Schema(description = "业务编码")
    @Size(max = 128, message = "bizCode长度不能超过128")
    private String bizCode;

    @ArraySchema(schema = @Schema(implementation = WorkflowDefinitionNodeETO.class))
    @Valid
    @NotEmpty(message = "nodes不能为空")
    private List<WorkflowDefinitionNodeETO> nodes;

    @ArraySchema(schema = @Schema(implementation = WorkflowTransitionETO.class))
    @Valid
    @NotEmpty(message = "transitions不能为空")
    private List<WorkflowTransitionETO> transitions;
}

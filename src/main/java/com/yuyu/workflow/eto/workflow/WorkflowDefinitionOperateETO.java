package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流定义单主键操作参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作流定义单主键操作参数")
public class WorkflowDefinitionOperateETO extends UserContextParam {

    @Schema(description = "流程定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id不能为空")
    private Long id;
}

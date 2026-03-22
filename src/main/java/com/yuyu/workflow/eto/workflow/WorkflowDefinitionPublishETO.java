package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发布流程定义参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "发布流程定义参数")
public class WorkflowDefinitionPublishETO extends UserContextParam {

    @Schema(description = "流程定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id不能为空")
    private Long id;
}

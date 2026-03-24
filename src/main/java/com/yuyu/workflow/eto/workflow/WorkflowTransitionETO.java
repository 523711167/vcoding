package com.yuyu.workflow.eto.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流程连线参数。
 */
@Data
@Schema(description = "流程连线参数")
public class WorkflowTransitionETO {

    @Schema(description = "来源节点编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "fromNodeCode不能为空")
    @Size(max = 64, message = "fromNodeCode长度不能超过64")
    private String fromNodeCode;

    @Schema(description = "目标节点编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "toNodeCode不能为空")
    @Size(max = 64, message = "toNodeCode长度不能超过64")
    private String toNodeCode;

    @Schema(description = "条件表达式")
    @Size(max = 512, message = "conditionExpr长度不能超过512")
    private String conditionExpr;

    @Schema(description = "是否默认分支：0=否 1=是")
    private Integer isDefault;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "连线标签")
    @Size(max = 64, message = "label长度不能超过64")
    private String label;
}

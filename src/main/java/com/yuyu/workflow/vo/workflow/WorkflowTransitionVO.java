package com.yuyu.workflow.vo.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程连线视图对象。
 */
@Data
@Schema(description = "流程连线视图对象")
public class WorkflowTransitionVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "流程定义ID")
    private Long definitionId;

    @Schema(description = "来源节点ID")
    private Long fromNodeId;

    @Schema(description = "来源节点编码")
    private String fromNodeCode;

    @Schema(description = "目标节点ID")
    private Long toNodeId;

    @Schema(description = "目标节点编码")
    private String toNodeCode;

    @Schema(description = "条件表达式")
    private String conditionExpr;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "连线标签")
    private String label;
}

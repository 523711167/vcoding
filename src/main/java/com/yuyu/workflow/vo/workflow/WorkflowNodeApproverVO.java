package com.yuyu.workflow.vo.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 节点审批人视图对象。
 */
@Data
@Schema(description = "节点审批人视图对象")
public class WorkflowNodeApproverVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "节点ID")
    private Long nodeId;

    @Schema(description = "审批人类型")
    private String approverType;

    @Schema(description = "审批人类型说明")
    private String approverTypeMsg;

    @Schema(description = "审批人值")
    private String approverValue;

    @Schema(description = "顺序值")
    private Integer sortOrder;
}

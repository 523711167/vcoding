package com.yuyu.workflow.service.model.workflow;

/**
 * 开启流程后的当前节点审核人信息。
 */
public record WorkflowStartApproverResult(
        Long approverId,
        String approverName,
        String status,
        Integer isActive,
        Integer sortOrder,
        String relationType
) {
}

package com.yuyu.workflow.service.model.workflow;

/**
 * 开启流程结果。
 */
public record WorkflowStartResult(
        Long bizApplyId,
        Long workflowInstanceId,
        WorkflowStartCurrentNodeResult currentNode
) {
}

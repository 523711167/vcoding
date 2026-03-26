package com.yuyu.workflow.service.model.workflow;

import java.util.List;

/**
 * 开启流程后的当前运行节点信息。
 */
public record WorkflowStartCurrentNodeResult(
        Long nodeInstanceId,
        Long nodeId,
        String nodeName,
        String nodeType,
        String status,
        String approveMode,
        List<WorkflowStartApproverResult> approverList
) {
}

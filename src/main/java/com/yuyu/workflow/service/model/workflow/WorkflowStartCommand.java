package com.yuyu.workflow.service.model.workflow;

/**
 * 开启流程命令。
 */
public record WorkflowStartCommand(
        Long bizApplyId,
        Long currentUserId
) {
}

package com.yuyu.workflow.service.model.workflow;

/**
 * 工作流后继路由树连线。
 */
public record WorkflowRouteEdge(
        Long transitionId,
        String label,
        String conditionExpr,
        Integer isDefault,
        Integer priority,
        Long fromNodeId,
        String fromNodeName,
        String fromNodeType,
        Long toNodeId,
        String toNodeName,
        String toNodeType,
        WorkflowRouteNode nextNode
) {
}

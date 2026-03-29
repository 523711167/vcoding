package com.yuyu.workflow.service.model.workflow;

import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.service.WorkflowRouteTreeBuilder.WorkflowGraphContext;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 工作流后继路由树节点。
 */
public record WorkflowRouteNode(
        Long nodeId,
        String nodeName,
        String nodeType,
        List<WorkflowRouteEdge> routes
) {

    /**
     * 根据节点ID查找该节点的下一跳节点。
     *
     * <p>若当前节点不存在，直接抛出异常。</p>
     * <p>若当前节点没有下一跳，返回 {@code null}。</p>
     * <p>若当前节点为条件节点且存在多条直接出边，直接抛出异常。</p>
     */
    public WorkflowNode findNextHopNode(Long currentNodeId, WorkflowGraphContext context) {
        WorkflowRouteNode currentNode = findNode(currentNodeId);
        if (currentNode == null) {
            throw new BizException("当前节点不存在");
        }
        if (currentNode.routes() == null || currentNode.routes().isEmpty()) {
            return null;
        }
        if (WorkflowNodeTypeEnum.isCondition(currentNode.nodeType()) && currentNode.routes().size() > 1) {
            throw new BizException("条件节点存在多条出边，无法直接确定下一跳");
        }

        WorkflowRouteNode nextNode = currentNode.routes().get(0).nextNode();
        if (nextNode == null) {
            return null;
        }
        if (context == null || context.nodeMap() == null) {
            return null;
        }
        return context.nodeMap().get(nextNode.nodeId());
    }

    /**
     * 在当前路由树中递归查找指定节点。
     */
    private WorkflowRouteNode findNode(Long currentNodeId) {
        if (Objects.equals(nodeId, currentNodeId)) {
            return this;
        }
        for (WorkflowRouteEdge route : routes == null ? Collections.<WorkflowRouteEdge>emptyList() : routes) {
            WorkflowRouteNode nextNode = route.nextNode();
            if (nextNode == null) {
                continue;
            }
            WorkflowRouteNode targetNode = nextNode.findNode(currentNodeId);
            if (targetNode != null) {
                return targetNode;
            }
        }
        return null;
    }
}

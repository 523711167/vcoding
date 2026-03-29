package com.yuyu.workflow.service;

import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.service.model.workflow.WorkflowRouteEdge;
import com.yuyu.workflow.service.model.workflow.WorkflowRouteNode;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流后继路由树构建器。
 */
@Component
public class WorkflowRouteTreeBuilder {

    /**
     * 基于节点和连线快照构建图上下文。
     */
    public WorkflowGraphContext buildGraphContext(List<WorkflowNode> nodeList,
                                                  List<WorkflowTransition> transitionList) {
        Map<Long, WorkflowNode> nodeMap = nodeList.stream()
                .collect(Collectors.toMap(WorkflowNode::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<WorkflowTransition>> transitionsByFromNodeId = transitionList.stream()
                .collect(Collectors.groupingBy(
                        WorkflowTransition::getFromNodeId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return new WorkflowGraphContext(nodeMap, transitionsByFromNodeId);
    }

    /**
     * 构建当前节点的后继路由树，保留全部直接出边并递归展开到审批节点或结束节点。
     */
    public WorkflowRouteNode buildNextRouteTree(Long currentDefinitionNodeId,
                                                WorkflowGraphContext context) {
        WorkflowNode currentNode = context.nodeMap().get(currentDefinitionNodeId);
        if (currentNode == null) {
            throw new BizException("当前节点不存在");
        }
        return buildRouteNode(currentNode, context, new LinkedHashSet<>());
    }


    /**
     * 返回根节点的所有直接路由。
     */
    public List<WorkflowRouteEdge> getDirectRoutes(WorkflowRouteNode root) {
        if (root == null || CollectionUtils.isEmpty(root.routes())) {
            return Collections.emptyList();
        }
        return root.routes();
    }

    /**
     * 返回路由树中的审批叶子节点。
     */
    public List<WorkflowRouteNode> findLeafApprovalNodes(WorkflowRouteNode root) {
        if (root == null) {
            return Collections.emptyList();
        }
        LinkedHashMap<Long, WorkflowRouteNode> result = new LinkedHashMap<>();
        collectLeafApprovalNodes(root, result);
        return new ArrayList<>(result.values());
    }


    /**
     * 递归构建路由树节点。
     */
    private WorkflowRouteNode buildRouteNode(WorkflowNode currentNode,
                                             WorkflowGraphContext context,
                                             Set<Long> pathVisited) {
        Set<Long> currentPathVisited = new LinkedHashSet<>(pathVisited);
        currentPathVisited.add(currentNode.getId());
        List<WorkflowRouteEdge> routes = context.transitionsByFromNodeId().getOrDefault(currentNode.getId(), Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(WorkflowTransition::getPriority, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(WorkflowTransition::getId, Comparator.nullsLast(Long::compareTo)))
                .map(transition -> buildRouteEdge(transition, context, currentPathVisited))
                .filter(Objects::nonNull)
                .toList();
        return new WorkflowRouteNode(currentNode.getId(), currentNode.getName(), currentNode.getNodeType(), routes);
    }

    /**
     * 构建单条路由边快照。
     */
    private WorkflowRouteEdge buildRouteEdge(WorkflowTransition transition,
                                             WorkflowGraphContext context,
                                             Set<Long> pathVisited) {
        WorkflowNode targetNode = context.nodeMap().get(transition.getToNodeId());
        if (targetNode == null) {
            return null;
        }
        WorkflowRouteNode nextNode = buildNextRouteChildNode(targetNode, context, pathVisited);
        return new WorkflowRouteEdge(
                transition.getId(),
                transition.getLabel(),
                transition.getConditionExpr(),
                transition.getIsDefault(),
                transition.getPriority(),
                transition.getFromNodeId(),
                transition.getFromNodeName(),
                transition.getFromNodeType(),
                transition.getToNodeId(),
                transition.getToNodeName(),
                transition.getToNodeType(),
                nextNode
        );
    }

    /**
     * 构建后继节点，审批节点和结束节点在这里终止递归。
     */
    private WorkflowRouteNode buildNextRouteChildNode(WorkflowNode targetNode,
                                                      WorkflowGraphContext context,
                                                      Set<Long> pathVisited) {
        if (pathVisited.contains(targetNode.getId())
                || WorkflowNodeTypeEnum.isApproval(targetNode.getNodeType())
                || WorkflowNodeTypeEnum.isEnd(targetNode.getNodeType())) {
            return new WorkflowRouteNode(
                    targetNode.getId(),
                    targetNode.getName(),
                    targetNode.getNodeType(),
                    Collections.emptyList()
            );
        }
        return buildRouteNode(targetNode, context, pathVisited);
    }

    /**
     * 递归收集审批叶子节点。
     */
    private void collectLeafApprovalNodes(WorkflowRouteNode currentNode,
                                          Map<Long, WorkflowRouteNode> result) {
        if (currentNode == null) {
            return;
        }
        if (WorkflowNodeTypeEnum.isApproval(currentNode.nodeType()) && CollectionUtils.isEmpty(currentNode.routes())) {
            result.putIfAbsent(currentNode.nodeId(), currentNode);
            return;
        }
        for (WorkflowRouteEdge route : currentNode.routes()) {
            collectLeafApprovalNodes(route.nextNode(), result);
        }
    }

    /**
     * 工作流图静态上下文，只承载节点和连线结构。
     */
    public record WorkflowGraphContext(
            Map<Long, WorkflowNode> nodeMap,
            Map<Long, List<WorkflowTransition>> transitionsByFromNodeId
    ) {
    }
}

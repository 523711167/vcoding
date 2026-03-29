package com.yuyu.workflow.service;

import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.service.model.workflow.WorkflowRouteEdge;
import com.yuyu.workflow.service.model.workflow.WorkflowRouteNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工作流路由树构建器测试。
 */
class WorkflowRouteTreeBuilderTests {

    private WorkflowRouteTreeBuilder workflowRouteTreeBuilder;

    @BeforeEach
    void setUp() {
        workflowRouteTreeBuilder = new WorkflowRouteTreeBuilder();
    }

    @Test
    void shouldBuildNestedRouteTreeAndKeepRouteSnapshot() {
        WorkflowRouteNode root = buildRouteTree(
                1L,
                List.of(
                        buildNode(1L, "当前审批", WorkflowNodeTypeEnum.APPROVAL.getCode()),
                        buildNode(2L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode()),
                        buildNode(3L, "并行拆分", WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode()),
                        buildNode(4L, "直属审批", WorkflowNodeTypeEnum.APPROVAL.getCode()),
                        buildNode(5L, "财务审批", WorkflowNodeTypeEnum.APPROVAL.getCode()),
                        buildNode(6L, "二次判断", WorkflowNodeTypeEnum.CONDITION.getCode()),
                        buildNode(7L, "结束", WorkflowNodeTypeEnum.END.getCode()),
                        buildNode(8L, "人事审批", WorkflowNodeTypeEnum.APPROVAL.getCode())
                ),
                List.of(
                        buildTransition(11L, 1L, "当前审批", WorkflowNodeTypeEnum.APPROVAL.getCode(),
                                2L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode(), null, 0, 1, "进入判断"),
                        buildTransition(12L, 2L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                3L, "并行拆分", WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode(), "amount > 1000", 0, 10, "大额"),
                        buildTransition(13L, 2L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                4L, "直属审批", WorkflowNodeTypeEnum.APPROVAL.getCode(), "amount <= 1000", 1, 20, "小额"),
                        buildTransition(14L, 3L, "并行拆分", WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode(),
                                5L, "财务审批", WorkflowNodeTypeEnum.APPROVAL.getCode(), "needFinance", 0, 30, "财务线"),
                        buildTransition(15L, 3L, "并行拆分", WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode(),
                                6L, "二次判断", WorkflowNodeTypeEnum.CONDITION.getCode(), "needHr", 1, 40, "人事线"),
                        buildTransition(16L, 6L, "二次判断", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                7L, "结束", WorkflowNodeTypeEnum.END.getCode(), "skip", 0, 50, "直接结束"),
                        buildTransition(17L, 6L, "二次判断", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                8L, "人事审批", WorkflowNodeTypeEnum.APPROVAL.getCode(), "pass", 1, 60, "继续审批")
                )
        );

        assertEquals(1L, root.nodeId());
        assertEquals(1, root.routes().size());
        WorkflowRouteNode conditionNode = root.routes().get(0).nextNode();
        assertNotNull(conditionNode);
        assertEquals(2L, conditionNode.nodeId());
        assertEquals(2, conditionNode.routes().size());

        WorkflowRouteEdge toSplitRoute = conditionNode.routes().get(0);
        assertEquals("amount > 1000", toSplitRoute.conditionExpr());
        assertEquals(0, toSplitRoute.isDefault());
        assertEquals(10, toSplitRoute.priority());
        assertEquals("金额判断", toSplitRoute.fromNodeName());
        assertEquals("并行拆分", toSplitRoute.toNodeName());

        WorkflowRouteNode splitNode = toSplitRoute.nextNode();
        assertNotNull(splitNode);
        assertEquals(3L, splitNode.nodeId());
        assertEquals(2, splitNode.routes().size());
        assertEquals(5L, splitNode.routes().get(0).nextNode().nodeId());
        assertEquals(6L, splitNode.routes().get(1).nextNode().nodeId());

        WorkflowRouteNode nestedConditionNode = splitNode.routes().get(1).nextNode();
        assertEquals(2, nestedConditionNode.routes().size());
        assertEquals(7L, nestedConditionNode.routes().get(0).nextNode().nodeId());
        assertEquals(8L, nestedConditionNode.routes().get(1).nextNode().nodeId());

        List<WorkflowRouteNode> approvalLeaves = workflowRouteTreeBuilder.findLeafApprovalNodes(root);
        assertEquals(List.of(4L, 5L, 8L),
                approvalLeaves.stream()
                        .map(WorkflowRouteNode::nodeId)
                        .sorted()
                        .toList());
    }

    @Test
    void shouldReturnEmptyRoutesWhenCurrentNodeHasNoOutgoingTransition() {
        WorkflowRouteNode root = buildRouteTree(
                20L,
                List.of(buildNode(20L, "空节点", WorkflowNodeTypeEnum.CONDITION.getCode())),
                List.of()
        );

        assertEquals(20L, root.nodeId());
        assertTrue(root.routes().isEmpty());
        assertTrue(workflowRouteTreeBuilder.getDirectRoutes(root).isEmpty());
    }

    @Test
    void shouldReturnNextHopNodeForNormalNode() {
        WorkflowNode currentNode = buildNode(101L, "审批节点", WorkflowNodeTypeEnum.APPROVAL.getCode());
        WorkflowNode endNode = buildNode(102L, "结束节点", WorkflowNodeTypeEnum.END.getCode());
        endNode.setApproveMode("OR");
        WorkflowRouteTreeBuilder.WorkflowGraphContext graphContext = workflowRouteTreeBuilder.buildGraphContext(
                List.of(currentNode, endNode),
                List.of(
                        buildTransition(1001L, 101L, "审批节点", WorkflowNodeTypeEnum.APPROVAL.getCode(),
                                102L, "结束节点", WorkflowNodeTypeEnum.END.getCode(), null, 0, 1, "结束")
                )
        );
        WorkflowRouteNode root = workflowRouteTreeBuilder.buildNextRouteTree(101L, graphContext);

        WorkflowNode nextHopNode = root.findNextHopNode(101L, graphContext);

        assertNotNull(nextHopNode);
        assertEquals(102L, nextHopNode.getId());
        assertEquals("结束节点", nextHopNode.getName());
        assertEquals(WorkflowNodeTypeEnum.END.getCode(), nextHopNode.getNodeType());
        assertEquals("OR", nextHopNode.getApproveMode());
    }

    @Test
    void shouldReturnNullWhenCurrentNodeHasNoNextHop() {
        WorkflowRouteTreeBuilder.WorkflowGraphContext graphContext = workflowRouteTreeBuilder.buildGraphContext(
                List.of(buildNode(201L, "孤立节点", WorkflowNodeTypeEnum.APPROVAL.getCode())),
                List.of()
        );
        WorkflowRouteNode root = workflowRouteTreeBuilder.buildNextRouteTree(201L, graphContext);

        assertNull(root.findNextHopNode(201L, graphContext));
    }

    @Test
    void shouldThrowWhenCurrentNodeDoesNotExist() {
        WorkflowRouteTreeBuilder.WorkflowGraphContext graphContext = workflowRouteTreeBuilder.buildGraphContext(
                List.of(buildNode(201L, "孤立节点", WorkflowNodeTypeEnum.APPROVAL.getCode())),
                List.of()
        );
        WorkflowRouteNode root = workflowRouteTreeBuilder.buildNextRouteTree(201L, graphContext);

        BizException exception = assertThrows(BizException.class, () -> root.findNextHopNode(999L, graphContext));

        assertEquals("当前节点不存在", exception.getMessage());
    }

    @Test
    void shouldThrowWhenConditionNodeHasMultipleOutgoingRoutes() {
        WorkflowRouteTreeBuilder.WorkflowGraphContext graphContext = workflowRouteTreeBuilder.buildGraphContext(
                List.of(
                        buildNode(301L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode()),
                        buildNode(302L, "审批A", WorkflowNodeTypeEnum.APPROVAL.getCode()),
                        buildNode(303L, "审批B", WorkflowNodeTypeEnum.APPROVAL.getCode())
                ),
                List.of(
                        buildTransition(3001L, 301L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                302L, "审批A", WorkflowNodeTypeEnum.APPROVAL.getCode(), "a", 0, 10, "A"),
                        buildTransition(3002L, 301L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                303L, "审批B", WorkflowNodeTypeEnum.APPROVAL.getCode(), "b", 1, 20, "B")
                )
        );
        WorkflowRouteNode root = workflowRouteTreeBuilder.buildNextRouteTree(301L, graphContext);

        BizException exception = assertThrows(BizException.class, () -> root.findNextHopNode(301L, graphContext));

        assertEquals("条件节点存在多条出边，无法直接确定下一跳", exception.getMessage());
    }

    @Test
    void shouldSkipBrokenEdgeAndStopRecursiveCycle() {
        WorkflowRouteNode root = buildRouteTree(
                30L,
                List.of(
                        buildNode(30L, "条件A", WorkflowNodeTypeEnum.CONDITION.getCode()),
                        buildNode(31L, "条件B", WorkflowNodeTypeEnum.CONDITION.getCode())
                ),
                List.of(
                        buildTransition(21L, 30L, "条件A", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                31L, "条件B", WorkflowNodeTypeEnum.CONDITION.getCode(), "toB", 0, 10, "有效边"),
                        buildTransition(22L, 30L, "条件A", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                99L, "坏节点", WorkflowNodeTypeEnum.APPROVAL.getCode(), "broken", 0, 20, "坏边"),
                        buildTransition(23L, 31L, "条件B", WorkflowNodeTypeEnum.CONDITION.getCode(),
                                30L, "条件A", WorkflowNodeTypeEnum.CONDITION.getCode(), "back", 1, 30, "回环")
                )
        );

        assertEquals(1, root.routes().size());
        WorkflowRouteNode secondNode = root.routes().get(0).nextNode();
        assertNotNull(secondNode);
        assertEquals(31L, secondNode.nodeId());
        assertEquals(1, secondNode.routes().size());
        assertEquals(30L, secondNode.routes().get(0).nextNode().nodeId());
        assertTrue(secondNode.routes().get(0).nextNode().routes().isEmpty());
    }

    private WorkflowRouteNode buildRouteTree(Long currentDefinitionNodeId,
                                             List<WorkflowNode> nodeList,
                                             List<WorkflowTransition> transitionList) {
        WorkflowRouteTreeBuilder.WorkflowGraphContext graphContext =
                workflowRouteTreeBuilder.buildGraphContext(nodeList, transitionList);
        return workflowRouteTreeBuilder.buildNextRouteTree(currentDefinitionNodeId, graphContext);
    }

    private WorkflowNode buildNode(Long id, String name, String nodeType) {
        WorkflowNode node = new WorkflowNode();
        node.setId(id);
        node.setName(name);
        node.setNodeType(nodeType);
        return node;
    }

    private WorkflowTransition buildTransition(Long id,
                                               Long fromNodeId,
                                               String fromNodeName,
                                               String fromNodeType,
                                               Long toNodeId,
                                               String toNodeName,
                                               String toNodeType,
                                               String conditionExpr,
                                               Integer isDefault,
                                               Integer priority,
                                               String label) {
        WorkflowTransition transition = new WorkflowTransition();
        transition.setId(id);
        transition.setFromNodeId(fromNodeId);
        transition.setFromNodeName(fromNodeName);
        transition.setFromNodeType(fromNodeType);
        transition.setToNodeId(toNodeId);
        transition.setToNodeName(toNodeName);
        transition.setToNodeType(toNodeType);
        transition.setConditionExpr(conditionExpr);
        transition.setIsDefault(isDefault);
        transition.setPriority(priority);
        transition.setLabel(label);
        return transition;
    }
}

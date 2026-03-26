package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowApprovalActionEnum;
import com.yuyu.workflow.common.enums.WorkflowApproverTypeEnum;
import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeApproverInstanceStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeApproverRelationTypeEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.convert.WorkflowNodeApproverInstanceStructMapper;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.entity.BizDefinitionRoleRel;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.UserRoleRel;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.mapper.BizDefinitionMapper;
import com.yuyu.workflow.mapper.BizDefinitionRoleRelMapper;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserDeptRelExpandMapper;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.UserRoleRelMapper;
import com.yuyu.workflow.mapper.WorkflowDefinitionMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverDeptExpandMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.mapper.WorkflowNodeInstanceMapper;
import com.yuyu.workflow.mapper.WorkflowTransitionMapper;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.service.WorkflowApprovalRecordService;
import com.yuyu.workflow.service.WorkflowInstanceService;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
import com.yuyu.workflow.service.WorkflowNodeInstanceService;
import com.yuyu.workflow.service.model.workflow.WorkflowStartCommand;
import com.yuyu.workflow.service.model.workflow.WorkflowStartResult;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/**
 * 工作流发起服务测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowLaunchServiceImplTests {

    @Mock
    private BizApplyService bizApplyService;
    @Mock
    private WorkflowInstanceService workflowInstanceService;
    @Mock
    private WorkflowNodeInstanceService workflowNodeInstanceService;
    @Mock
    private WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService;
    @Mock
    private WorkflowApprovalRecordService workflowApprovalRecordService;
    @Mock
    private BizDefinitionMapper bizDefinitionMapper;
    @Mock
    private BizDefinitionRoleRelMapper bizDefinitionRoleRelMapper;
    @Mock
    private WorkflowDefinitionMapper workflowDefinitionMapper;
    @Mock
    private WorkflowNodeMapper workflowNodeMapper;
    @Mock
    private WorkflowTransitionMapper workflowTransitionMapper;
    @Mock
    private WorkflowNodeApproverMapper workflowNodeApproverMapper;
    @Mock
    private WorkflowNodeApproverDeptExpandMapper workflowNodeApproverDeptExpandMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRoleRelMapper userRoleRelMapper;
    @Mock
    private UserDeptRelMapper userDeptRelMapper;
    @Mock
    private UserDeptRelExpandMapper userDeptRelExpandMapper;
    @Mock
    private UserDeptMapper userDeptMapper;
    @Mock
    private ObjectMapperUtils objectMapperUtils;
    @Mock
    private WorkflowNodeApproverInstanceStructMapper workflowNodeApproverInstanceStructMapper;
    @Mock
    private WorkflowNodeInstanceMapper workflowNodeInstanceMapper;

    private WorkflowLaunchServiceImpl workflowLaunchService;

    @BeforeEach
    void setUp() {
        workflowLaunchService = new WorkflowLaunchServiceImpl(
                bizApplyService,
                workflowInstanceService,
                workflowNodeInstanceService,
                workflowNodeApproverInstanceService,
                workflowApprovalRecordService,
                bizDefinitionMapper,
                bizDefinitionRoleRelMapper,
                workflowDefinitionMapper,
                workflowNodeMapper,
                workflowTransitionMapper,
                workflowNodeApproverMapper,
                workflowNodeApproverDeptExpandMapper,
                userMapper,
                userRoleRelMapper,
                userDeptRelMapper,
                userDeptRelExpandMapper,
                userDeptMapper,
                objectMapperUtils,
                workflowNodeApproverInstanceStructMapper,
                workflowNodeInstanceMapper
        );
    }

    @Test
    void shouldSubmitBizApplyToFirstApprovalNode() {
        BizApply bizApply = buildBizApply();
        WorkflowDefinition workflowDefinition = buildWorkflowDefinition();
        WorkflowNode startNode = buildNode(10L, "开始", WorkflowNodeTypeEnum.START.getCode());
        WorkflowNode approvalNode = buildNode(11L, "直属领导审批", WorkflowNodeTypeEnum.APPROVAL.getCode());
        WorkflowTransition transition = buildTransition(10L, 11L, null, 0, 1);
        WorkflowNodeApprover approver = buildApprover(11L, WorkflowApproverTypeEnum.USER.getCode(), "2", 1);

        mockCommonSubmitContext(bizApply, workflowDefinition, List.of(startNode, approvalNode), List.of(transition), List.of(approver));
        when(userMapper.selectById(2L)).thenReturn(buildUser(2L, "leader", "直属领导"));

        WorkflowStartResult result = workflowLaunchService.startWorkflow(new WorkflowStartCommand(1L, 1L));

        assertEquals(1L, result.bizApplyId());
        assertEquals(900L, result.workflowInstanceId());
        assertEquals("直属领导审批", result.currentNode().nodeName());
        assertEquals("APPROVAL", result.currentNode().nodeType());
        assertEquals(1, result.currentNode().approverList().size());
        assertEquals(2L, result.currentNode().approverList().get(0).approverId());
        assertEquals("直属领导", result.currentNode().approverList().get(0).approverName());
        assertEquals("PENDING", result.currentNode().approverList().get(0).status());

        ArgumentCaptor<WorkflowInstance> workflowInstanceCaptor = ArgumentCaptor.forClass(WorkflowInstance.class);
        verify(workflowInstanceService).save(workflowInstanceCaptor.capture());
        assertEquals(10L, workflowInstanceCaptor.getValue().getBizId());

        ArgumentCaptor<WorkflowInstance> updatedWorkflowInstanceCaptor = ArgumentCaptor.forClass(WorkflowInstance.class);
        verify(workflowInstanceService).updateById(updatedWorkflowInstanceCaptor.capture());
        assertEquals(11L, updatedWorkflowInstanceCaptor.getValue().getCurrentNodeId());
        assertEquals("直属领导审批", updatedWorkflowInstanceCaptor.getValue().getCurrentNodeName());
        assertEquals(WorkflowNodeTypeEnum.APPROVAL.getCode(), updatedWorkflowInstanceCaptor.getValue().getCurrentNodeType());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkflowNodeApproverInstance>> approverCaptor = ArgumentCaptor.forClass(List.class);
        verify(workflowNodeApproverInstanceService).saveBatch(approverCaptor.capture());
        verify(workflowNodeInstanceService, times(1)).save(any(WorkflowNodeInstance.class));
        WorkflowNodeApproverInstance approverInstance = approverCaptor.getValue().get(0);
        assertEquals("直属领导审批", approverInstance.getNodeName());
        assertEquals(WorkflowNodeTypeEnum.APPROVAL.getCode(), approverInstance.getNodeType());
        assertEquals(WorkflowNodeApproverRelationTypeEnum.ORIGINAL.getCode(), approverInstance.getRelationType());
        assertEquals(WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode(), approverInstance.getStatus());

        ArgumentCaptor<WorkflowApprovalRecord> submitCaptor = ArgumentCaptor.forClass(WorkflowApprovalRecord.class);
        verify(workflowApprovalRecordService).save(submitCaptor.capture());
        assertEquals(WorkflowApprovalActionEnum.SUBMIT.getCode(), submitCaptor.getValue().getAction());
        assertEquals(1L, submitCaptor.getValue().getOperatorId());
    }

    @Test
    void shouldRouteConditionNodeBeforeActivatingApprovalNode() {
        BizApply bizApply = buildBizApply();
        bizApply.setFormData("{\"amount\":6000}");
        WorkflowDefinition workflowDefinition = buildWorkflowDefinition();
        WorkflowNode startNode = buildNode(10L, "开始", WorkflowNodeTypeEnum.START.getCode());
        WorkflowNode conditionNode = buildNode(11L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode());
        WorkflowNode approvalNode = buildNode(12L, "财务审批", WorkflowNodeTypeEnum.APPROVAL.getCode());
        WorkflowNode endNode = buildNode(13L, "结束", WorkflowNodeTypeEnum.END.getCode());
        WorkflowTransition startTransition = buildTransition(10L, 11L, null, 0, 1);
        WorkflowTransition matchTransition = buildTransition(11L, 12L, "amount >= 5000", 0, 10);
        WorkflowTransition defaultTransition = buildTransition(11L, 13L, "amount < 5000", 1, 20);
        WorkflowNodeApprover approver = buildApprover(12L, WorkflowApproverTypeEnum.USER.getCode(), "2", 1);

        mockCommonSubmitContext(
                bizApply,
                workflowDefinition,
                List.of(startNode, conditionNode, approvalNode, endNode),
                List.of(startTransition, matchTransition, defaultTransition),
                List.of(approver)
        );
        when(userMapper.selectById(2L)).thenReturn(buildUser(2L, "finance", "财务经理"));
        when(objectMapperUtils.fromJson(org.mockito.ArgumentMatchers.eq("{\"amount\":6000}"), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>>any()))
                .thenReturn(java.util.Map.of("amount", 6000));

        workflowLaunchService.startWorkflow(new WorkflowStartCommand(1L, 1L));

        verify(workflowNodeInstanceService, times(2)).save(any(WorkflowNodeInstance.class));
        ArgumentCaptor<WorkflowInstance> updatedWorkflowInstanceCaptor = ArgumentCaptor.forClass(WorkflowInstance.class);
        verify(workflowInstanceService).updateById(updatedWorkflowInstanceCaptor.capture());
        assertEquals(12L, updatedWorkflowInstanceCaptor.getValue().getCurrentNodeId());
        assertEquals("财务审批", updatedWorkflowInstanceCaptor.getValue().getCurrentNodeName());
        assertEquals(WorkflowNodeTypeEnum.APPROVAL.getCode(), updatedWorkflowInstanceCaptor.getValue().getCurrentNodeType());
        ArgumentCaptor<List<WorkflowApprovalRecord>> recordCaptor = ArgumentCaptor.forClass(List.class);
        verify(workflowApprovalRecordService).saveBatch(recordCaptor.capture());
        List<WorkflowApprovalRecord> systemRecords = recordCaptor.getValue();
        assertEquals(1, systemRecords.size());
        assertEquals(WorkflowApprovalActionEnum.ROUTE.getCode(), systemRecords.get(0).getAction());
        assertNotNull(systemRecords.get(0).getToNodeId());
    }

    @Test
    void shouldClearCurrentNodeSnapshotWhenWorkflowFinishesImmediately() {
        BizApply bizApply = buildBizApply();
        bizApply.setFormData("{\"amount\":1000}");
        WorkflowDefinition workflowDefinition = buildWorkflowDefinition();
        WorkflowNode startNode = buildNode(10L, "开始", WorkflowNodeTypeEnum.START.getCode());
        WorkflowNode conditionNode = buildNode(11L, "金额判断", WorkflowNodeTypeEnum.CONDITION.getCode());
        WorkflowNode endNode = buildNode(13L, "结束", WorkflowNodeTypeEnum.END.getCode());
        WorkflowTransition startTransition = buildTransition(10L, 11L, null, 0, 1);
        WorkflowTransition defaultTransition = buildTransition(11L, 13L, "amount < 5000", 1, 20);

        mockCommonSubmitContext(
                bizApply,
                workflowDefinition,
                List.of(startNode, conditionNode, endNode),
                List.of(startTransition, defaultTransition),
                Collections.emptyList()
        );

        workflowLaunchService.startWorkflow(new WorkflowStartCommand(1L, 1L));

        ArgumentCaptor<WorkflowInstance> updatedWorkflowInstanceCaptor = ArgumentCaptor.forClass(WorkflowInstance.class);
        verify(workflowInstanceService).updateById(updatedWorkflowInstanceCaptor.capture());
        assertEquals("APPROVED", updatedWorkflowInstanceCaptor.getValue().getStatus());
        assertNull(updatedWorkflowInstanceCaptor.getValue().getCurrentNodeId());
        assertNull(updatedWorkflowInstanceCaptor.getValue().getCurrentNodeName());
        assertNull(updatedWorkflowInstanceCaptor.getValue().getCurrentNodeType());
        assertNotNull(updatedWorkflowInstanceCaptor.getValue().getFinishedAt());
    }

    @Test
    void shouldInheritParallelBranchRootIdForNodesInsideParallelBranch() {
        BizApply bizApply = buildBizApply();
        WorkflowDefinition workflowDefinition = buildWorkflowDefinition();
        WorkflowNode startNode = buildNode(10L, "开始", WorkflowNodeTypeEnum.START.getCode());
        WorkflowNode splitNode = buildNode(11L, "并行拆分", WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode());
        WorkflowNode conditionNode = buildNode(12L, "条件判断", WorkflowNodeTypeEnum.CONDITION.getCode());
        WorkflowNode branchApprovalNode = buildNode(13L, "分支B审批", WorkflowNodeTypeEnum.APPROVAL.getCode());
        WorkflowNode nestedApprovalNode = buildNode(14L, "分支A审批", WorkflowNodeTypeEnum.APPROVAL.getCode());
        WorkflowTransition startTransition = buildTransition(10L, 11L, null, 0, 1);
        WorkflowTransition splitToCondition = buildTransition(11L, 12L, null, 0, 10);
        WorkflowTransition splitToApproval = buildTransition(11L, 13L, null, 0, 20);
        WorkflowTransition conditionToApproval = buildTransition(12L, 14L, null, 0, 30);
        WorkflowNodeApprover branchApprover = buildApprover(13L, WorkflowApproverTypeEnum.USER.getCode(), "2", 1);
        WorkflowNodeApprover nestedApprover = buildApprover(14L, WorkflowApproverTypeEnum.USER.getCode(), "3", 1);

        mockCommonSubmitContext(
                bizApply,
                workflowDefinition,
                List.of(startNode, splitNode, conditionNode, branchApprovalNode, nestedApprovalNode),
                List.of(startTransition, splitToCondition, splitToApproval, conditionToApproval),
                List.of(branchApprover, nestedApprover)
        );
        when(userMapper.selectById(2L)).thenReturn(buildUser(2L, "branch_b", "分支B审批人"));
        when(userMapper.selectById(3L)).thenReturn(buildUser(3L, "branch_a", "分支A审批人"));

        workflowLaunchService.startWorkflow(new WorkflowStartCommand(1L, 1L));

        ArgumentCaptor<WorkflowNodeInstance> nodeInstanceCaptor = ArgumentCaptor.forClass(WorkflowNodeInstance.class);
        verify(workflowNodeInstanceService, times(4)).save(nodeInstanceCaptor.capture());
        List<WorkflowNodeInstance> nodeInstances = nodeInstanceCaptor.getAllValues();

        WorkflowNodeInstance splitInstance = nodeInstances.get(0);
        WorkflowNodeInstance conditionInstance = nodeInstances.get(1);
        WorkflowNodeInstance nestedApprovalInstance = nodeInstances.get(2);
        WorkflowNodeInstance branchApprovalInstance = nodeInstances.get(3);

        assertNull(splitInstance.getParallelBranchRootId());
        assertEquals(splitInstance.getId(), conditionInstance.getParallelBranchRootId());
        assertEquals(splitInstance.getId(), nestedApprovalInstance.getParallelBranchRootId());
        assertEquals(splitInstance.getId(), branchApprovalInstance.getParallelBranchRootId());
    }

    private void mockCommonSubmitContext(BizApply bizApply,
                                         WorkflowDefinition workflowDefinition,
                                         List<WorkflowNode> nodeList,
                                         List<WorkflowTransition> transitions,
                                         List<WorkflowNodeApprover> approvers) {
        when(bizApplyService.getByIdOrThrow(1L)).thenReturn(bizApply);
        when(bizDefinitionMapper.selectById(10L)).thenReturn(buildBizDefinition());
        when(workflowDefinitionMapper.selectById(100L)).thenReturn(workflowDefinition);
        when(userRoleRelMapper.selectList(any())).thenReturn(List.of(buildUserRoleRel(1L, 8L)));
        when(bizDefinitionRoleRelMapper.selectList(any())).thenReturn(List.of(buildBizDefinitionRoleRel(10L, 8L)));
        when(workflowNodeMapper.selectList(any())).thenReturn(nodeList);
        when(workflowTransitionMapper.selectList(any())).thenReturn(transitions);
        when(workflowNodeApproverMapper.selectList(any())).thenReturn(approvers);
        when(workflowNodeApproverDeptExpandMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "applicant", "申请人"));
        when(userDeptRelMapper.selectOne(any())).thenReturn(null);
        AtomicLong workflowInstanceId = new AtomicLong(900L);
        AtomicLong nodeInstanceId = new AtomicLong(1000L);
        doAnswer(invocation -> {
            WorkflowInstance entity = invocation.getArgument(0);
            entity.setId(workflowInstanceId.get());
            return true;
        }).when(workflowInstanceService).save(any(WorkflowInstance.class));
        doAnswer(invocation -> {
            WorkflowNodeInstance entity = invocation.getArgument(0);
            entity.setId(nodeInstanceId.getAndIncrement());
            return true;
        }).when(workflowNodeInstanceService).save(any(WorkflowNodeInstance.class));
    }

    private BizApply buildBizApply() {
        BizApply bizApply = new BizApply();
        bizApply.setId(1L);
        bizApply.setBizDefinitionId(10L);
        bizApply.setTitle("报销申请");
        bizApply.setBizStatus("DRAFT");
        bizApply.setApplicantId(1L);
        bizApply.setApplicantName("申请人");
        bizApply.setFormData("{}");
        return bizApply;
    }

    private BizDefinition buildBizDefinition() {
        BizDefinition bizDefinition = new BizDefinition();
        bizDefinition.setId(10L);
        bizDefinition.setBizCode("EXPENSE");
        bizDefinition.setWorkflowDefinitionId(100L);
        bizDefinition.setStatus(CommonStatusEnum.ENABLED.getId());
        return bizDefinition;
    }

    private WorkflowDefinition buildWorkflowDefinition() {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setId(100L);
        workflowDefinition.setCode("EXPENSE_WF");
        workflowDefinition.setName("报销流程");
        workflowDefinition.setStatus(WorkflowDefinitionStatusEnum.PUBLISHED.getId());
        return workflowDefinition;
    }

    private WorkflowNode buildNode(Long id, String name, String nodeType) {
        WorkflowNode node = new WorkflowNode();
        node.setId(id);
        node.setDefinitionId(100L);
        node.setName(name);
        node.setNodeType(nodeType);
        return node;
    }

    private WorkflowTransition buildTransition(Long fromNodeId, Long toNodeId, String conditionExpr, Integer isDefault, Integer priority) {
        WorkflowTransition transition = new WorkflowTransition();
        transition.setFromNodeId(fromNodeId);
        transition.setToNodeId(toNodeId);
        transition.setConditionExpr(conditionExpr);
        transition.setIsDefault(isDefault);
        transition.setPriority(priority);
        return transition;
    }

    private WorkflowNodeApprover buildApprover(Long nodeId, String approverType, String approverValue, Integer sortOrder) {
        WorkflowNodeApprover approver = new WorkflowNodeApprover();
        approver.setId(nodeId + 100);
        approver.setNodeId(nodeId);
        approver.setApproverType(approverType);
        approver.setApproverValue(approverValue);
        approver.setSortOrder(sortOrder);
        return approver;
    }

    private User buildUser(Long id, String username, String realName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setStatus(CommonStatusEnum.ENABLED.getId());
        return user;
    }

    private UserRoleRel buildUserRoleRel(Long userId, Long roleId) {
        UserRoleRel relation = new UserRoleRel();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        return relation;
    }

    private BizDefinitionRoleRel buildBizDefinitionRoleRel(Long bizDefinitionId, Long roleId) {
        BizDefinitionRoleRel relation = new BizDefinitionRoleRel();
        relation.setBizDefinitionId(bizDefinitionId);
        relation.setRoleId(roleId);
        return relation;
    }
}

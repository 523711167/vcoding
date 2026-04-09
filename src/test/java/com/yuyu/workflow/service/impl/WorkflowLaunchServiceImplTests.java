package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import com.yuyu.workflow.config.JacksonConfig;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.eto.workflow.WorkflowDelegateETO;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.service.WorkflowApprovalRecordService;
import com.yuyu.workflow.service.WorkflowInstanceService;
import com.yuyu.workflow.service.WorkflowNodeService;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
import com.yuyu.workflow.service.WorkflowNodeInstanceService;
import com.yuyu.workflow.service.WorkflowDefinitionService;
import com.yuyu.workflow.service.WorkflowLaunchService;
import com.yuyu.workflow.service.WorkflowParallelScopeService;
import com.yuyu.workflow.service.WorkflowTransitionService;
import com.yuyu.workflow.struct.WorkflowLaunchStructMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作流发起服务测试。
 */
class WorkflowLaunchServiceImplTests {

    private WorkflowLaunchService workflowLaunchService;
    private WorkflowInstanceService workflowInstanceService;
    private WorkflowNodeInstanceService workflowNodeInstanceService;
    private WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService;
    private WorkflowApprovalRecordService workflowApprovalRecordService;
    private UserMapper userMapper;
    private Method findMatchConditionNodeMethod;
    private Constructor<?> auditContextConstructor;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapperUtils objectMapperUtils = new ObjectMapperUtils(new JacksonConfig().objectMapper());
        workflowInstanceService = mock(WorkflowInstanceService.class);
        workflowNodeInstanceService = mock(WorkflowNodeInstanceService.class);
        workflowNodeApproverInstanceService = mock(WorkflowNodeApproverInstanceService.class);
        workflowApprovalRecordService = mock(WorkflowApprovalRecordService.class);
        userMapper = mock(UserMapper.class);
        workflowLaunchService = new WorkflowLaunchServiceImpl(
                null,
                workflowInstanceService,
                workflowNodeInstanceService,
                workflowNodeApproverInstanceService,
                workflowApprovalRecordService,
                null,
                null,
                null,
                userMapper,
                null,
                null,
                objectMapperUtils,
                null,
                mock(WorkflowDefinitionService.class),
                mock(WorkflowParallelScopeService.class),
                mock(WorkflowLaunchStructMapper.class),
                mock(WorkflowNodeService.class),
                mock(WorkflowTransitionService.class)
        );

        Class<?> auditContextClass =
                Class.forName("com.yuyu.workflow.service.impl.WorkflowLaunchServiceImpl$AuditContext");
        auditContextConstructor = auditContextClass.getDeclaredConstructors()[0];
        auditContextConstructor.setAccessible(true);

        findMatchConditionNodeMethod = WorkflowLaunchServiceImpl.class.getDeclaredMethod(
                "findMatchConditionNode",
                Long.class,
                auditContextClass
        );
        findMatchConditionNodeMethod.setAccessible(true);
    }

    @Test
    void shouldReturnMatchedConditionNode() throws Exception {
        WorkflowTransition hitTransition = buildTransition(1L, 11L, "amount >= 5000", 0);
        WorkflowTransition defaultTransition = buildTransition(2L, 12L, null, 1);

        WorkflowNode hitNode = buildNode(11L, "财务审批", "APPROVAL");
        WorkflowNode defaultNode = buildNode(12L, "结束", "END");

        Object auditContext = buildAuditContext(
                "{\"amount\":6000}",
                Map.of(hitNode.getId(), hitNode, defaultNode.getId(), defaultNode),
                List.of(hitTransition, defaultTransition)
        );

        WorkflowNode result = (WorkflowNode) findMatchConditionNodeMethod.invoke(
                workflowLaunchService,
                1L,
                auditContext
        );

        assertEquals(11L, result.getId());
        assertEquals("财务审批", result.getName());
    }

    @Test
    void shouldFallbackToDefaultConditionNode() throws Exception {
        WorkflowTransition missTransition = buildTransition(1L, 11L, "amount >= 5000", 0);
        WorkflowTransition defaultTransition = buildTransition(2L, 12L, null, 1);

        WorkflowNode missNode = buildNode(11L, "财务审批", "APPROVAL");
        WorkflowNode defaultNode = buildNode(12L, "结束", "END");

        Object auditContext = buildAuditContext(
                "{\"amount\":1000}",
                Map.of(missNode.getId(), missNode, defaultNode.getId(), defaultNode),
                List.of(missTransition, defaultTransition)
        );

        WorkflowNode result = (WorkflowNode) findMatchConditionNodeMethod.invoke(
                workflowLaunchService,
                1L,
                auditContext
        );

        assertEquals(12L, result.getId());
        assertEquals("结束", result.getName());
    }

    @Test
    void shouldThrowWhenNoConditionMatchedAndNoDefaultBranch() throws Exception {
        WorkflowTransition missTransition = buildTransition(1L, 11L, "amount >= 5000", 0);
        WorkflowNode missNode = buildNode(11L, "财务审批", "APPROVAL");

        Object auditContext = buildAuditContext(
                "{\"amount\":1000}",
                Map.of(missNode.getId(), missNode),
                List.of(missTransition)
        );

        Throwable throwable = assertThrows(Throwable.class, () -> findMatchConditionNodeMethod.invoke(
                workflowLaunchService,
                1L,
                auditContext
        ));

        Throwable target = throwable.getCause() == null ? throwable : throwable.getCause();
        assertEquals(BizException.class, target.getClass());
        assertEquals("条件节点未匹配到分支且未配置默认分支", target.getMessage());
    }

    @Test
    void shouldDelegateWorkflowTaskToAnotherUser() {
        WorkflowDelegateETO eto = new WorkflowDelegateETO();
        eto.setInstanceId(1001L);
        eto.setNodeInstanceId(2001L);
        eto.setApproverInstanceId(3001L);
        eto.setDelegateToUserId(9L);
        eto.setCurrentUserId(1L);
        eto.setCurrentUsername("admin");
        eto.setComment("请帮忙处理");

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setId(1001L);
        workflowInstance.setStatus("RUNNING");

        WorkflowNodeInstance nodeInstance = new WorkflowNodeInstance();
        nodeInstance.setId(2001L);
        nodeInstance.setInstanceId(1001L);
        nodeInstance.setStatus("ACTIVE");
        nodeInstance.setDefinitionNodeId(88L);
        nodeInstance.setDefinitionNodeName("直属领导审批");
        nodeInstance.setDefinitionNodeType("APPROVAL");

        WorkflowNodeApproverInstance approverInstance = new WorkflowNodeApproverInstance();
        approverInstance.setId(3001L);
        approverInstance.setNodeInstanceId(2001L);
        approverInstance.setApproverId(1L);
        approverInstance.setStatus("PENDING");
        approverInstance.setIsActive(1);

        User delegateUser = new User();
        delegateUser.setId(9L);
        delegateUser.setUsername("zhangsan");
        delegateUser.setRealName("张三");
        delegateUser.setStatus(1);

        when(workflowInstanceService.getByIdOrThrow(1001L)).thenReturn(workflowInstance);
        when(workflowNodeInstanceService.getById(2001L)).thenReturn(nodeInstance);
        when(workflowNodeApproverInstanceService.getById(3001L)).thenReturn(approverInstance);
        when(workflowNodeApproverInstanceService.list(org.mockito.ArgumentMatchers.<Wrapper<WorkflowNodeApproverInstance>>any()))
                .thenReturn(List.of(approverInstance));
        when(userMapper.selectById(9L)).thenReturn(delegateUser);

        workflowLaunchService.delegate(eto);

        verify(workflowNodeApproverInstanceService).saveApproverInstancesForDelegate(approverInstance, delegateUser, "请帮忙处理");
        verify(workflowApprovalRecordService).recordForDelegate(eto, nodeInstance, delegateUser);
    }

    @Test
    void shouldRejectDelegateWhenTargetAlreadyInCurrentNodeChain() {
        WorkflowDelegateETO eto = new WorkflowDelegateETO();
        eto.setInstanceId(1001L);
        eto.setNodeInstanceId(2001L);
        eto.setApproverInstanceId(3001L);
        eto.setDelegateToUserId(9L);
        eto.setCurrentUserId(1L);

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setId(1001L);
        workflowInstance.setStatus("RUNNING");

        WorkflowNodeInstance nodeInstance = new WorkflowNodeInstance();
        nodeInstance.setId(2001L);
        nodeInstance.setInstanceId(1001L);
        nodeInstance.setStatus("ACTIVE");

        WorkflowNodeApproverInstance currentApproverInstance = new WorkflowNodeApproverInstance();
        currentApproverInstance.setId(3001L);
        currentApproverInstance.setNodeInstanceId(2001L);
        currentApproverInstance.setApproverId(1L);
        currentApproverInstance.setStatus("PENDING");
        currentApproverInstance.setIsActive(1);

        WorkflowNodeApproverInstance existingTargetApprover = new WorkflowNodeApproverInstance();
        existingTargetApprover.setId(3002L);
        existingTargetApprover.setNodeInstanceId(2001L);
        existingTargetApprover.setApproverId(9L);

        User delegateUser = new User();
        delegateUser.setId(9L);
        delegateUser.setUsername("zhangsan");
        delegateUser.setStatus(1);

        when(workflowInstanceService.getByIdOrThrow(1001L)).thenReturn(workflowInstance);
        when(workflowNodeInstanceService.getById(2001L)).thenReturn(nodeInstance);
        when(workflowNodeApproverInstanceService.getById(3001L)).thenReturn(currentApproverInstance);
        when(workflowNodeApproverInstanceService.list(org.mockito.ArgumentMatchers.<Wrapper<WorkflowNodeApproverInstance>>any()))
                .thenReturn(List.of(currentApproverInstance, existingTargetApprover));
        when(userMapper.selectById(9L)).thenReturn(delegateUser);

        BizException exception = assertThrows(BizException.class, () -> workflowLaunchService.delegate(eto));

        assertEquals("转交目标用户已在当前节点审批链中", exception.getMessage());
    }

    private Object buildAuditContext(String formData,
                                     Map<Long, WorkflowNode> nodeMap,
                                     List<WorkflowTransition> transitionList) throws Exception {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setFormData(formData);

        return auditContextConstructor.newInstance(
                null,
                workflowInstance,
                null,
                null,
                List.of(),
                null,
                new LinkedHashMap<>(nodeMap),
                Map.of(1L, transitionList)
        );
    }

    private WorkflowTransition buildTransition(Long id, Long toNodeId, String conditionExpr, Integer isDefault) {
        WorkflowTransition transition = new WorkflowTransition();
        transition.setId(id);
        transition.setToNodeId(toNodeId);
        transition.setConditionExpr(conditionExpr);
        transition.setIsDefault(isDefault);
        return transition;
    }

    private WorkflowNode buildNode(Long id, String name, String nodeType) {
        WorkflowNode workflowNode = new WorkflowNode();
        workflowNode.setId(id);
        workflowNode.setName(name);
        workflowNode.setNodeType(nodeType);
        return workflowNode;
    }
}

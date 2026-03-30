package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import com.yuyu.workflow.config.JacksonConfig;
import com.yuyu.workflow.convert.WorkflowDefinitionStructMapper;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionCreateETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionDisableETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionPublishETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionUpdateETO;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.WorkflowDefinitionMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.mapper.WorkflowTransitionMapper;
import com.yuyu.workflow.service.WorkflowNodeApproverService;
import com.yuyu.workflow.service.WorkflowNodeApproverDeptExpandService;
import com.yuyu.workflow.vo.workflow.WorkflowDefinitionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流程定义服务测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowDefinitionServiceImplTests {

    @Mock
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Mock
    private WorkflowNodeMapper workflowNodeMapper;

    @Mock
    private WorkflowNodeApproverMapper workflowNodeApproverMapper;

    @Mock
    private WorkflowTransitionMapper workflowTransitionMapper;

    @Mock
    private WorkflowNodeApproverDeptExpandService workflowNodeApproverDeptExpandService;

    @Mock
    private UserDeptMapper userDeptMapper;

    private WorkflowNodeApproverService workflowNodeApproverService;
    private WorkflowDefinitionServiceImpl workflowDefinitionService;

    @BeforeEach
    void setUp() {
        WorkflowDefinitionStructMapper workflowDefinitionStructMapper =
                Mappers.getMapper(WorkflowDefinitionStructMapper.class);
        ObjectMapperUtils objectMapperUtils = new ObjectMapperUtils(new JacksonConfig().objectMapper());
        workflowNodeApproverService = new WorkflowNodeApproverServiceImpl(workflowNodeApproverMapper);
        workflowDefinitionService = new WorkflowDefinitionServiceImpl(
                workflowDefinitionMapper,
                workflowNodeMapper,
                workflowNodeApproverMapper,
                workflowNodeApproverService,
                workflowTransitionMapper,
                workflowDefinitionStructMapper,
                workflowNodeApproverDeptExpandService,
                userDeptMapper,
                objectMapperUtils
        );
    }

    @Test
    void shouldCreateDefinitionAndResolveTransitionNodeIds() {
        WorkflowDefinitionCreateETO eto = new WorkflowDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setName("请假审批");
        eto.setCode("LEAVE_APPROVAL");
        eto.setWorkFlowJson(buildWorkflowJson("2"));

        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setId(100L);
        entity.setName("请假审批");
        entity.setCode("LEAVE_APPROVAL");
        entity.setWorkflowJson(eto.getWorkFlowJson());
        entity.setStatus(WorkflowDefinitionStatusEnum.DRAFT.getId());

        when(workflowDefinitionMapper.selectOne(any())).thenReturn(null);
        when(workflowDefinitionMapper.selectMaxVersionByCode("LEAVE_APPROVAL")).thenReturn(2);
        when(workflowDefinitionMapper.selectById(100L)).thenReturn(entity);
        when(workflowNodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(workflowTransitionMapper.selectList(any())).thenReturn(Collections.emptyList());

        UserDept dept = new UserDept();
        dept.setId(2L);
        dept.setStatus(1);
        when(userDeptMapper.selectById(2L)).thenReturn(dept);

        doAnswer(invocation -> {
            WorkflowDefinition argument = invocation.getArgument(0);
            argument.setId(100L);
            return 1;
        }).when(workflowDefinitionMapper).insert(any(WorkflowDefinition.class));
        doAnswer(new NodeInsertAnswer()).when(workflowNodeMapper).insert(any(WorkflowNode.class));
        doAnswer(new ApproverInsertAnswer()).when(workflowNodeApproverMapper).insert(any(WorkflowNodeApprover.class));

        WorkflowDefinitionVO result = workflowDefinitionService.create(eto);

        ArgumentCaptor<WorkflowDefinition> definitionCaptor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(workflowDefinitionMapper).insert(definitionCaptor.capture());
        assertEquals(3, definitionCaptor.getValue().getVersion());
        assertEquals(WorkflowDefinitionStatusEnum.DRAFT.getId(), definitionCaptor.getValue().getStatus());
        assertEquals(9L, definitionCaptor.getValue().getCreatedBy());
        assertEquals(eto.getWorkFlowJson(), definitionCaptor.getValue().getWorkflowJson());

        ArgumentCaptor<WorkflowNode> nodeCaptor = ArgumentCaptor.forClass(WorkflowNode.class);
        verify(workflowNodeMapper, times(3)).insert(nodeCaptor.capture());
        WorkflowNode approvalNode = nodeCaptor.getAllValues().get(1);
        assertEquals(90, approvalNode.getTimeoutMinutes());
        assertEquals(30, approvalNode.getRemindMinutes());

        ArgumentCaptor<WorkflowTransition> transitionCaptor = ArgumentCaptor.forClass(WorkflowTransition.class);
        verify(workflowTransitionMapper, times(2)).insert(transitionCaptor.capture());
        assertEquals(List.of(1001L, 1002L),
                transitionCaptor.getAllValues().stream().map(WorkflowTransition::getFromNodeId).toList());
        assertEquals(List.of(1002L, 1003L),
                transitionCaptor.getAllValues().stream().map(WorkflowTransition::getToNodeId).toList());
        assertEquals(List.of("开始", "主管审批"),
                transitionCaptor.getAllValues().stream().map(WorkflowTransition::getFromNodeName).toList());
        assertEquals(List.of("START", "APPROVAL"),
                transitionCaptor.getAllValues().stream().map(WorkflowTransition::getFromNodeType).toList());
        assertEquals(List.of("主管审批", "结束"),
                transitionCaptor.getAllValues().stream().map(WorkflowTransition::getToNodeName).toList());
        assertEquals(List.of("APPROVAL", "END"),
                transitionCaptor.getAllValues().stream().map(WorkflowTransition::getToNodeType).toList());
        assertEquals(List.of(0, 0),
                transitionCaptor.getAllValues().stream().map(WorkflowTransition::getIsDefault).toList());
        ArgumentCaptor<WorkflowNodeApprover> approverCaptor = ArgumentCaptor.forClass(WorkflowNodeApprover.class);
        verify(workflowNodeApproverMapper).insert(approverCaptor.capture());
        assertEquals(100L, approverCaptor.getValue().getDefinitionId());
        verify(workflowNodeApproverDeptExpandService).rebuildByApproverIds(List.of(2001L));
        assertEquals(eto.getWorkFlowJson(), result.getWorkFlowJson());
    }

    @Test
    void shouldPersistParallelSplitOwnerNodeId() {
        WorkflowDefinitionCreateETO eto = new WorkflowDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setName("并行审批");
        eto.setCode("PARALLEL_APPROVAL");
        eto.setWorkFlowJson(buildParallelOwnerWorkflowJson());

        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setId(102L);
        entity.setName("并行审批");
        entity.setCode("PARALLEL_APPROVAL");
        entity.setWorkflowJson(eto.getWorkFlowJson());
        entity.setStatus(WorkflowDefinitionStatusEnum.DRAFT.getId());

        when(workflowDefinitionMapper.selectOne(any())).thenReturn(null);
        when(workflowDefinitionMapper.selectMaxVersionByCode("PARALLEL_APPROVAL")).thenReturn(null);
        when(workflowDefinitionMapper.selectById(102L)).thenReturn(entity);
        when(workflowNodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(workflowTransitionMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userDeptMapper.selectById(2L)).thenReturn(buildEnabledDept(2L));

        doAnswer(invocation -> {
            WorkflowDefinition argument = invocation.getArgument(0);
            argument.setId(102L);
            return 1;
        }).when(workflowDefinitionMapper).insert(any(WorkflowDefinition.class));
        doAnswer(new NodeInsertAnswer()).when(workflowNodeMapper).insert(any(WorkflowNode.class));
        doAnswer(new ApproverInsertAnswer()).when(workflowNodeApproverMapper).insert(any(WorkflowNodeApprover.class));

        workflowDefinitionService.create(eto);

        ArgumentCaptor<WorkflowNode> nodeCaptor = ArgumentCaptor.forClass(WorkflowNode.class);
        verify(workflowNodeMapper, times(7)).insert(nodeCaptor.capture());
        List<WorkflowNode> nodeList = nodeCaptor.getAllValues();

        WorkflowNode startNode = nodeList.get(0);
        WorkflowNode splitNode = nodeList.get(1);
        WorkflowNode approvalNodeA = nodeList.get(2);
        WorkflowNode conditionNode = nodeList.get(3);
        WorkflowNode approvalNodeB = nodeList.get(4);
        WorkflowNode joinNode = nodeList.get(5);
        WorkflowNode endNode = nodeList.get(6);

        assertNull(startNode.getParallelSplitNodeId());
        assertNull(splitNode.getParallelSplitNodeId());
        assertEquals(splitNode.getId(), approvalNodeA.getParallelSplitNodeId());
        assertEquals(splitNode.getId(), conditionNode.getParallelSplitNodeId());
        assertEquals(splitNode.getId(), approvalNodeB.getParallelSplitNodeId());
        assertEquals(splitNode.getId(), joinNode.getParallelSplitNodeId());
        assertNull(endNode.getParallelSplitNodeId());
    }

    @Test
    void shouldRejectCommaSeparatedApproverValue() {
        WorkflowDefinitionCreateETO eto = new WorkflowDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setName("请假审批");
        eto.setCode("LEAVE_APPROVAL");
        eto.setWorkFlowJson(buildWorkflowJson("1,2"));

        BizException exception = assertThrows(BizException.class, () -> workflowDefinitionService.create(eto));

        assertEquals("approverValue不允许使用逗号拼接多个值", exception.getMessage());
    }

    @Test
    void shouldRejectConditionNodeWithoutDefaultBranchWhenMultipleTransitions() {
        WorkflowDefinitionCreateETO eto = new WorkflowDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setName("报销审批");
        eto.setCode("EXPENSE_APPROVAL");
        eto.setWorkFlowJson(buildConditionWorkflowJson(false, false));
        when(userDeptMapper.selectById(2L)).thenReturn(buildEnabledDept(2L));

        BizException exception = assertThrows(BizException.class, () -> workflowDefinitionService.create(eto));

        assertEquals("金额判断必须且只能配置一条默认分支", exception.getMessage());
    }

    @Test
    void shouldRejectParallelSplitWithoutDefaultBranchWhenMultipleTransitions() {
        WorkflowDefinitionCreateETO eto = new WorkflowDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setName("报销审批");
        eto.setCode("EXPENSE_APPROVAL");
        eto.setWorkFlowJson(buildParallelSplitWorkflowJson(false, false));
        when(userDeptMapper.selectById(2L)).thenReturn(buildEnabledDept(2L));

        BizException exception = assertThrows(BizException.class, () -> workflowDefinitionService.create(eto));

        assertEquals("并行拆分必须且只能配置一条默认分支", exception.getMessage());
    }

    @Test
    void shouldRejectConditionNodeWithMultipleDefaultBranches() {
        WorkflowDefinitionCreateETO eto = new WorkflowDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setName("报销审批");
        eto.setCode("EXPENSE_APPROVAL");
        eto.setWorkFlowJson(buildConditionWorkflowJson(true, true));
        when(userDeptMapper.selectById(2L)).thenReturn(buildEnabledDept(2L));

        BizException exception = assertThrows(BizException.class, () -> workflowDefinitionService.create(eto));

        assertEquals("金额判断必须且只能配置一条默认分支", exception.getMessage());
    }

    @Test
    void shouldParseSnakeCaseDefaultFlagWhenCreatingDefinition() {
        WorkflowDefinitionCreateETO eto = new WorkflowDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setName("报销审批");
        eto.setCode("EXPENSE_APPROVAL");
        eto.setWorkFlowJson(buildConditionWorkflowJsonWithDefaultKey("is_default", false, true));

        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setId(101L);
        entity.setName("报销审批");
        entity.setCode("EXPENSE_APPROVAL");
        entity.setWorkflowJson(eto.getWorkFlowJson());
        entity.setStatus(WorkflowDefinitionStatusEnum.DRAFT.getId());

        when(workflowDefinitionMapper.selectOne(any())).thenReturn(null);
        when(workflowDefinitionMapper.selectMaxVersionByCode("EXPENSE_APPROVAL")).thenReturn(null);
        when(workflowDefinitionMapper.selectById(101L)).thenReturn(entity);
        when(workflowNodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(workflowTransitionMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userDeptMapper.selectById(2L)).thenReturn(buildEnabledDept(2L));

        doAnswer(invocation -> {
            WorkflowDefinition argument = invocation.getArgument(0);
            argument.setId(101L);
            return 1;
        }).when(workflowDefinitionMapper).insert(any(WorkflowDefinition.class));
        doAnswer(new NodeInsertAnswer()).when(workflowNodeMapper).insert(any(WorkflowNode.class));
        doAnswer(new ApproverInsertAnswer()).when(workflowNodeApproverMapper).insert(any(WorkflowNodeApprover.class));

        workflowDefinitionService.create(eto);

        ArgumentCaptor<WorkflowTransition> transitionCaptor = ArgumentCaptor.forClass(WorkflowTransition.class);
        verify(workflowTransitionMapper, times(4)).insert(transitionCaptor.capture());
        assertEquals(List.of(0, 0, 1, 0),
                transitionCaptor.getAllValues().stream().map(WorkflowTransition::getIsDefault).toList());
    }

    @Test
    void shouldUpdateDraftDefinitionDirectly() {
        WorkflowDefinitionUpdateETO eto = new WorkflowDefinitionUpdateETO();
        eto.setId(10L);
        eto.setCurrentUserId(9L);
        eto.setName("请假审批-草稿");
        eto.setDescription("草稿直接修改");
        eto.setWorkFlowJson(buildWorkflowJson("2"));

        WorkflowDefinition oldEntity = buildDefinition(10L, WorkflowDefinitionStatusEnum.DRAFT.getId(), "LEAVE_APPROVAL", 1, "old");
        WorkflowDefinition updatedEntity = buildDefinition(10L, WorkflowDefinitionStatusEnum.DRAFT.getId(), "LEAVE_APPROVAL", 1, eto.getWorkFlowJson());
        when(workflowDefinitionMapper.selectById(10L)).thenReturn(oldEntity, updatedEntity);
        when(workflowNodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(workflowTransitionMapper.selectList(any())).thenReturn(Collections.emptyList());

        UserDept dept = new UserDept();
        dept.setId(2L);
        dept.setStatus(1);
        when(userDeptMapper.selectById(2L)).thenReturn(dept);

        doAnswer(new NodeInsertAnswer()).when(workflowNodeMapper).insert(any(WorkflowNode.class));
        doAnswer(new ApproverInsertAnswer()).when(workflowNodeApproverMapper).insert(any(WorkflowNodeApprover.class));

        WorkflowDefinitionVO result = workflowDefinitionService.update(eto);

        ArgumentCaptor<WorkflowDefinition> updateCaptor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(workflowDefinitionMapper).updateById(updateCaptor.capture());
        assertEquals(10L, updateCaptor.getValue().getId());
        assertEquals(1, updateCaptor.getValue().getVersion());
        assertEquals(eto.getWorkFlowJson(), updateCaptor.getValue().getWorkflowJson());
        verify(workflowDefinitionMapper, never()).insert(any(WorkflowDefinition.class));
        assertEquals(10L, result.getId());
    }

    @Test
    void shouldCreateNewDraftVersionWhenUpdatingPublishedDefinition() {
        WorkflowDefinitionUpdateETO eto = new WorkflowDefinitionUpdateETO();
        eto.setId(11L);
        eto.setCurrentUserId(9L);
        eto.setName("请假审批-已发布");
        eto.setDescription("发布版本修改");
        eto.setWorkFlowJson(buildWorkflowJson("2"));

        WorkflowDefinition oldEntity = buildDefinition(11L, WorkflowDefinitionStatusEnum.PUBLISHED.getId(), "LEAVE_APPROVAL", 1, "old");
        WorkflowDefinition newEntity = buildDefinition(200L, WorkflowDefinitionStatusEnum.DRAFT.getId(), "LEAVE_APPROVAL", 2, eto.getWorkFlowJson());

        when(workflowDefinitionMapper.selectById(11L)).thenReturn(oldEntity);
        when(workflowDefinitionMapper.selectMaxVersionByCode("LEAVE_APPROVAL")).thenReturn(1);
        when(workflowDefinitionMapper.selectById(200L)).thenReturn(newEntity);
        when(workflowDefinitionMapper.selectOne(any())).thenReturn(null);
        when(workflowNodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(workflowTransitionMapper.selectList(any())).thenReturn(Collections.emptyList());

        UserDept dept = new UserDept();
        dept.setId(2L);
        dept.setStatus(1);
        when(userDeptMapper.selectById(2L)).thenReturn(dept);

        doAnswer(invocation -> {
            WorkflowDefinition argument = invocation.getArgument(0);
            argument.setId(200L);
            return 1;
        }).when(workflowDefinitionMapper).insert(any(WorkflowDefinition.class));
        doAnswer(new NodeInsertAnswer()).when(workflowNodeMapper).insert(any(WorkflowNode.class));
        doAnswer(new ApproverInsertAnswer()).when(workflowNodeApproverMapper).insert(any(WorkflowNodeApprover.class));

        WorkflowDefinitionVO result = workflowDefinitionService.update(eto);

        ArgumentCaptor<WorkflowDefinition> insertCaptor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(workflowDefinitionMapper).insert(insertCaptor.capture());
        assertEquals(2, insertCaptor.getValue().getVersion());
        assertEquals(WorkflowDefinitionStatusEnum.DRAFT.getId(), insertCaptor.getValue().getStatus());
        assertEquals(9L, insertCaptor.getValue().getCreatedBy());
        verify(workflowDefinitionMapper, never()).updateById(any(WorkflowDefinition.class));
        verify(workflowNodeApproverDeptExpandService).rebuildByApproverIds(List.of(2001L));
        assertEquals(200L, result.getId());
    }

    @Test
    void shouldCreateNewDraftVersionWhenUpdatingDisabledDefinition() {
        WorkflowDefinitionUpdateETO eto = new WorkflowDefinitionUpdateETO();
        eto.setId(12L);
        eto.setCurrentUserId(9L);
        eto.setName("请假审批-已停用");
        eto.setDescription("停用版本修改");
        eto.setWorkFlowJson(buildWorkflowJson("2"));

        WorkflowDefinition disabledEntity = buildDefinition(12L, WorkflowDefinitionStatusEnum.DISABLED.getId(), "LEAVE_APPROVAL", 1, "old");
        WorkflowDefinition newEntity = buildDefinition(201L, WorkflowDefinitionStatusEnum.DRAFT.getId(), "LEAVE_APPROVAL", 3, eto.getWorkFlowJson());

        when(workflowDefinitionMapper.selectById(12L)).thenReturn(disabledEntity);
        when(workflowDefinitionMapper.selectMaxVersionByCode("LEAVE_APPROVAL")).thenReturn(2);
        when(workflowDefinitionMapper.selectById(201L)).thenReturn(newEntity);
        when(workflowDefinitionMapper.selectOne(any())).thenReturn(null);
        when(workflowNodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(workflowTransitionMapper.selectList(any())).thenReturn(Collections.emptyList());

        UserDept dept = new UserDept();
        dept.setId(2L);
        dept.setStatus(1);
        when(userDeptMapper.selectById(2L)).thenReturn(dept);

        doAnswer(invocation -> {
            WorkflowDefinition argument = invocation.getArgument(0);
            argument.setId(201L);
            return 1;
        }).when(workflowDefinitionMapper).insert(any(WorkflowDefinition.class));
        doAnswer(new NodeInsertAnswer()).when(workflowNodeMapper).insert(any(WorkflowNode.class));
        doAnswer(new ApproverInsertAnswer()).when(workflowNodeApproverMapper).insert(any(WorkflowNodeApprover.class));

        WorkflowDefinitionVO result = workflowDefinitionService.update(eto);

        ArgumentCaptor<WorkflowDefinition> insertCaptor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(workflowDefinitionMapper).insert(insertCaptor.capture());
        assertEquals(3, insertCaptor.getValue().getVersion());
        assertEquals(WorkflowDefinitionStatusEnum.DRAFT.getId(), insertCaptor.getValue().getStatus());
        verify(workflowDefinitionMapper, never()).updateById(any(WorkflowDefinition.class));
        assertEquals(201L, result.getId());
    }

    @Test
    void shouldRejectPublishingNonDraftDefinition() {
        WorkflowDefinitionPublishETO eto = new WorkflowDefinitionPublishETO();
        eto.setId(1L);

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(1L);
        definition.setStatus(WorkflowDefinitionStatusEnum.PUBLISHED.getId());

        when(workflowDefinitionMapper.selectById(1L)).thenReturn(definition);

        BizException exception = assertThrows(BizException.class, () -> workflowDefinitionService.publish(eto));

        assertEquals("仅草稿流程允许修改", exception.getMessage());
    }

    @Test
    void shouldRejectPublishingDraftDefinitionWithoutRequiredDefaultBranch() {
        WorkflowDefinitionPublishETO eto = new WorkflowDefinitionPublishETO();
        eto.setId(3L);

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(3L);
        definition.setCode("EXPENSE_APPROVAL");
        definition.setStatus(WorkflowDefinitionStatusEnum.DRAFT.getId());
        when(workflowDefinitionMapper.selectById(3L)).thenReturn(definition);

        WorkflowNode conditionNode = new WorkflowNode();
        conditionNode.setId(100L);
        conditionNode.setName("金额判断");
        conditionNode.setNodeType("CONDITION");
        when(workflowNodeMapper.selectList(any())).thenReturn(List.of(conditionNode));

        WorkflowTransition first = new WorkflowTransition();
        first.setId(1L);
        first.setDefinitionId(3L);
        first.setFromNodeId(100L);
        first.setToNodeId(101L);
        first.setIsDefault(0);
        first.setPriority(1);
        WorkflowTransition second = new WorkflowTransition();
        second.setId(2L);
        second.setDefinitionId(3L);
        second.setFromNodeId(100L);
        second.setToNodeId(102L);
        second.setIsDefault(0);
        second.setPriority(2);
        when(workflowTransitionMapper.selectList(any())).thenReturn(List.of(first, second));

        BizException exception = assertThrows(BizException.class, () -> workflowDefinitionService.publish(eto));

        assertEquals("金额判断必须且只能配置一条默认分支", exception.getMessage());
    }

    @Test
    void shouldDisableDefinitionWhenRequested() {
        WorkflowDefinitionDisableETO eto = new WorkflowDefinitionDisableETO();
        eto.setId(2L);

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(2L);
        definition.setStatus(WorkflowDefinitionStatusEnum.PUBLISHED.getId());

        when(workflowDefinitionMapper.selectById(2L)).thenReturn(definition);

        workflowDefinitionService.disable(eto);

        verify(workflowDefinitionMapper).updateById(any(WorkflowDefinition.class));
    }

    /**
     * 构造简化版流程设计 JSON。
     */
    private String buildWorkflowJson(String approverId) {
        return """
                {
                  "nodes": [
                    {
                      "id": "START",
                      "x": 120,
                      "y": 220,
                      "properties": {
                        "nodeRole": "START_END",
                        "approverIds": []
                      },
                      "text": {
                        "value": "开始"
                      }
                    },
                    {
                      "id": "LEADER_APPROVAL",
                      "x": 300,
                      "y": 220,
                      "properties": {
                        "nodeRole": "APPROVAL",
                        "approverType": "DEPT",
                        "approverIds": ["%s"],
                        "approveMode": "COUNTERSIGN",
                        "timeoutStrategy": "REMIND_ONLY",
                        "timeoutAfterMinutes": 90,
                        "remindAfterMinutes": 30
                      },
                      "text": {
                        "value": "主管审批"
                      }
                    },
                    {
                      "id": "END",
                      "x": 480,
                      "y": 220,
                      "properties": {
                        "nodeRole": "START_END",
                        "approverIds": []
                      },
                      "text": {
                        "value": "结束"
                      }
                    }
                  ],
                  "edges": [
                    {
                      "sourceNodeId": "START",
                      "targetNodeId": "LEADER_APPROVAL",
                      "properties": {
                        "priority": 1
                      },
                      "text": {
                        "value": "提交"
                      }
                    },
                    {
                      "sourceNodeId": "LEADER_APPROVAL",
                      "targetNodeId": "END",
                      "properties": {
                        "priority": 2
                      },
                      "text": {
                        "value": "结束"
                      }
                    }
                  ]
                }
                """.formatted(approverId);
    }

    /**
     * 构造条件节点多分支流程。
     */
    private String buildConditionWorkflowJson(boolean firstDefault, boolean secondDefault) {
        return buildConditionWorkflowJsonWithDefaultKey("is_default", firstDefault, secondDefault);
    }

    /**
     * 构造条件节点多分支流程，并指定默认分支字段名。
     */
    private String buildConditionWorkflowJsonWithDefaultKey(String defaultKey, boolean firstDefault, boolean secondDefault) {
        return """
                {
                  "nodes": [
                    {
                      "id": "START",
                      "x": 120,
                      "y": 220,
                      "properties": {
                        "nodeRole": "START_END",
                        "approverIds": []
                      },
                      "text": {
                        "value": "开始"
                      }
                    },
                    {
                      "id": "CONDITION_NODE",
                      "x": 300,
                      "y": 220,
                      "properties": {
                        "nodeRole": "CONDITION",
                        "approverIds": []
                      },
                      "text": {
                        "value": "金额判断"
                      }
                    },
                    {
                      "id": "APPROVAL_A",
                      "x": 480,
                      "y": 160,
                      "properties": {
                        "nodeRole": "APPROVAL",
                        "approverType": "DEPT",
                        "approverIds": ["2"],
                        "approveMode": "COUNTERSIGN"
                      },
                      "text": {
                        "value": "主管审批A"
                      }
                    },
                    {
                      "id": "END",
                      "x": 660,
                      "y": 220,
                      "properties": {
                        "nodeRole": "START_END",
                        "approverIds": []
                      },
                      "text": {
                        "value": "结束"
                      }
                    }
                  ],
                  "edges": [
                    {
                      "sourceNodeId": "START",
                      "targetNodeId": "CONDITION_NODE",
                      "properties": {
                        "priority": 1
                      },
                      "text": {
                        "value": "提交"
                      }
                    },
                    {
                      "sourceNodeId": "CONDITION_NODE",
                      "targetNodeId": "APPROVAL_A",
                      "properties": {
                        "expression": "amount > 5000",
                        "priority": 1,
                        "%s": %s
                      },
                      "text": {
                        "value": "金额大于5000"
                      }
                    },
                    {
                      "sourceNodeId": "CONDITION_NODE",
                      "targetNodeId": "END",
                      "properties": {
                        "priority": 99,
                        "%s": %s
                      },
                      "text": {
                        "value": "默认结束"
                      }
                    },
                    {
                      "sourceNodeId": "APPROVAL_A",
                      "targetNodeId": "END",
                      "properties": {
                        "priority": 2
                      },
                      "text": {
                        "value": "通过"
                      }
                    }
                  ]
                }
                """.formatted(defaultKey, firstDefault, defaultKey, secondDefault);
    }

    /**
     * 构造并行拆分多分支流程。
     */
    private String buildParallelSplitWorkflowJson(boolean firstDefault, boolean secondDefault) {
        return """
                {
                  "nodes": [
                    {
                      "id": "START",
                      "x": 120,
                      "y": 220,
                      "properties": {
                        "nodeRole": "START_END",
                        "approverIds": []
                      },
                      "text": {
                        "value": "开始"
                      }
                    },
                    {
                      "id": "PARALLEL_NODE",
                      "x": 300,
                      "y": 220,
                      "properties": {
                        "nodeRole": "PARALLEL_SPLIT",
                        "approverIds": []
                      },
                      "text": {
                        "value": "并行拆分"
                      }
                    },
                    {
                      "id": "APPROVAL_A",
                      "x": 480,
                      "y": 160,
                      "properties": {
                        "nodeRole": "APPROVAL",
                        "approverType": "DEPT",
                        "approverIds": ["2"],
                        "approveMode": "COUNTERSIGN"
                      },
                      "text": {
                        "value": "财务审批"
                      }
                    },
                    {
                      "id": "APPROVAL_B",
                      "x": 480,
                      "y": 280,
                      "properties": {
                        "nodeRole": "APPROVAL",
                        "approverType": "DEPT",
                        "approverIds": ["2"],
                        "approveMode": "COUNTERSIGN"
                      },
                      "text": {
                        "value": "人事审批"
                      }
                    },
                    {
                      "id": "END",
                      "x": 660,
                      "y": 220,
                      "properties": {
                        "nodeRole": "START_END",
                        "approverIds": []
                      },
                      "text": {
                        "value": "结束"
                      }
                    }
                  ],
                  "edges": [
                    {
                      "sourceNodeId": "START",
                      "targetNodeId": "PARALLEL_NODE",
                      "properties": {
                        "priority": 1
                      },
                      "text": {
                        "value": "提交"
                      }
                    },
                    {
                      "sourceNodeId": "PARALLEL_NODE",
                      "targetNodeId": "APPROVAL_A",
                      "properties": {
                        "expression": "amount > 5000",
                        "priority": 1,
                        "is_default": %s
                      },
                      "text": {
                        "value": "财务线"
                      }
                    },
                    {
                      "sourceNodeId": "PARALLEL_NODE",
                      "targetNodeId": "APPROVAL_B",
                      "properties": {
                        "expression": "department != 'FINANCE'",
                        "priority": 2,
                        "is_default": %s
                      },
                      "text": {
                        "value": "人事线"
                      }
                    },
                    {
                      "sourceNodeId": "APPROVAL_A",
                      "targetNodeId": "END",
                      "properties": {
                        "priority": 3
                      },
                      "text": {
                        "value": "通过A"
                      }
                    },
                    {
                      "sourceNodeId": "APPROVAL_B",
                      "targetNodeId": "END",
                      "properties": {
                        "priority": 4
                      },
                      "text": {
                        "value": "通过B"
                      }
                    }
                  ]
                }
                """.formatted(firstDefault, secondDefault);
    }

    /**
     * 构造包含并行拆分、条件分支和并行聚合的流程。
     */
    private String buildParallelOwnerWorkflowJson() {
        return """
                {
                  "nodes": [
                    {
                      "id": "START",
                      "x": 120,
                      "y": 220,
                      "properties": {
                        "nodeRole": "START_END",
                        "approverIds": []
                      },
                      "text": {
                        "value": "开始"
                      }
                    },
                    {
                      "id": "PARALLEL_SPLIT_NODE",
                      "x": 300,
                      "y": 220,
                      "properties": {
                        "nodeRole": "PARALLEL_SPLIT",
                        "approverIds": []
                      },
                      "text": {
                        "value": "并行拆分"
                      }
                    },
                    {
                      "id": "APPROVAL_A",
                      "x": 480,
                      "y": 140,
                      "properties": {
                        "nodeRole": "APPROVAL",
                        "approverType": "DEPT",
                        "approverIds": ["2"],
                        "approveMode": "COUNTERSIGN"
                      },
                      "text": {
                        "value": "审批A"
                      }
                    },
                    {
                      "id": "CONDITION_NODE",
                      "x": 480,
                      "y": 300,
                      "properties": {
                        "nodeRole": "CONDITION",
                        "approverIds": []
                      },
                      "text": {
                        "value": "条件判断"
                      }
                    },
                    {
                      "id": "APPROVAL_B",
                      "x": 660,
                      "y": 300,
                      "properties": {
                        "nodeRole": "APPROVAL",
                        "approverType": "DEPT",
                        "approverIds": ["2"],
                        "approveMode": "COUNTERSIGN"
                      },
                      "text": {
                        "value": "审批B"
                      }
                    },
                    {
                      "id": "PARALLEL_JOIN_NODE",
                      "x": 840,
                      "y": 220,
                      "properties": {
                        "nodeRole": "PARALLEL_JOIN",
                        "approverIds": []
                      },
                      "text": {
                        "value": "并行聚合"
                      }
                    },
                    {
                      "id": "END",
                      "x": 1020,
                      "y": 220,
                      "properties": {
                        "nodeRole": "START_END",
                        "approverIds": []
                      },
                      "text": {
                        "value": "结束"
                      }
                    }
                  ],
                  "edges": [
                    {
                      "sourceNodeId": "START",
                      "targetNodeId": "PARALLEL_SPLIT_NODE",
                      "properties": {
                        "priority": 1
                      },
                      "text": {
                        "value": "提交"
                      }
                    },
                    {
                      "sourceNodeId": "PARALLEL_SPLIT_NODE",
                      "targetNodeId": "APPROVAL_A",
                      "properties": {
                        "priority": 10,
                        "expression": "amount > 5000",
                        "is_default": false
                      },
                      "text": {
                        "value": "分支A"
                      }
                    },
                    {
                      "sourceNodeId": "PARALLEL_SPLIT_NODE",
                      "targetNodeId": "CONDITION_NODE",
                      "properties": {
                        "priority": 20,
                        "is_default": true
                      },
                      "text": {
                        "value": "分支B"
                      }
                    },
                    {
                      "sourceNodeId": "APPROVAL_A",
                      "targetNodeId": "PARALLEL_JOIN_NODE",
                      "properties": {
                        "priority": 30
                      },
                      "text": {
                        "value": "A完成"
                      }
                    },
                    {
                      "sourceNodeId": "CONDITION_NODE",
                      "targetNodeId": "APPROVAL_B",
                      "properties": {
                        "priority": 40,
                        "expression": "department != 'FINANCE'",
                        "is_default": true
                      },
                      "text": {
                        "value": "进入审批B"
                      }
                    },
                    {
                      "sourceNodeId": "APPROVAL_B",
                      "targetNodeId": "PARALLEL_JOIN_NODE",
                      "properties": {
                        "priority": 50
                      },
                      "text": {
                        "value": "B完成"
                      }
                    },
                    {
                      "sourceNodeId": "PARALLEL_JOIN_NODE",
                      "targetNodeId": "END",
                      "properties": {
                        "priority": 60
                      },
                      "text": {
                        "value": "结束"
                      }
                    }
                  ]
                }
                """;
    }

    /**
     * 构造流程定义对象。
     */
    private WorkflowDefinition buildDefinition(Long id, Integer status, String code, Integer version, String workflowJson) {
        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setId(id);
        entity.setName("请假审批");
        entity.setCode(code);
        entity.setVersion(version);
        entity.setWorkflowJson(workflowJson);
        entity.setStatus(status);
        return entity;
    }

    /**
     * 构造有效组织。
     */
    private UserDept buildEnabledDept(Long id) {
        UserDept dept = new UserDept();
        dept.setId(id);
        dept.setStatus(1);
        return dept;
    }

    /**
     * 模拟节点插入后回填主键。
     */
    static class NodeInsertAnswer implements org.mockito.stubbing.Answer<Integer> {

        private long id = 1000L;

        @Override
        public Integer answer(org.mockito.invocation.InvocationOnMock invocation) {
            WorkflowNode node = invocation.getArgument(0);
            id++;
            node.setId(id);
            return 1;
        }
    }

    /**
     * 模拟审批人插入后回填主键。
     */
    static class ApproverInsertAnswer implements org.mockito.stubbing.Answer<Integer> {

        private long id = 2000L;

        @Override
        public Integer answer(org.mockito.invocation.InvocationOnMock invocation) {
            WorkflowNodeApprover approver = invocation.getArgument(0);
            id++;
            approver.setId(id);
            return 1;
        }
    }
}

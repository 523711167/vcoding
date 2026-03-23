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

    private WorkflowDefinitionServiceImpl workflowDefinitionService;

    @BeforeEach
    void setUp() {
        WorkflowDefinitionStructMapper workflowDefinitionStructMapper =
                Mappers.getMapper(WorkflowDefinitionStructMapper.class);
        ObjectMapperUtils objectMapperUtils = new ObjectMapperUtils(new JacksonConfig().objectMapper());
        workflowDefinitionService = new WorkflowDefinitionServiceImpl(
                workflowDefinitionMapper,
                workflowNodeMapper,
                workflowNodeApproverMapper,
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
        verify(workflowNodeApproverDeptExpandService).rebuildByApproverIds(List.of(2001L));
        assertEquals(eto.getWorkFlowJson(), result.getWorkFlowJson());
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
    void shouldCreateNewPublishedVersionWhenUpdatingPublishedDefinition() {
        WorkflowDefinitionUpdateETO eto = new WorkflowDefinitionUpdateETO();
        eto.setId(11L);
        eto.setCurrentUserId(9L);
        eto.setName("请假审批-已发布");
        eto.setDescription("发布版本修改");
        eto.setWorkFlowJson(buildWorkflowJson("2"));

        WorkflowDefinition oldEntity = buildDefinition(11L, WorkflowDefinitionStatusEnum.PUBLISHED.getId(), "LEAVE_APPROVAL", 1, "old");
        WorkflowDefinition newEntity = buildDefinition(200L, WorkflowDefinitionStatusEnum.PUBLISHED.getId(), "LEAVE_APPROVAL", 2, eto.getWorkFlowJson());

        when(workflowDefinitionMapper.selectById(11L)).thenReturn(oldEntity);
        when(workflowDefinitionMapper.selectMaxVersionByCode("LEAVE_APPROVAL")).thenReturn(1);
        when(workflowDefinitionMapper.selectById(200L)).thenReturn(newEntity);
        when(workflowDefinitionMapper.selectList(any())).thenReturn(List.of(oldEntity));
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
        assertEquals(WorkflowDefinitionStatusEnum.PUBLISHED.getId(), insertCaptor.getValue().getStatus());
        assertEquals(9L, insertCaptor.getValue().getCreatedBy());

        ArgumentCaptor<WorkflowDefinition> updateCaptor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(workflowDefinitionMapper, times(2)).updateById(updateCaptor.capture());
        assertEquals(List.of(11L, 200L), updateCaptor.getAllValues().stream().map(WorkflowDefinition::getId).toList());
        assertEquals(List.of(WorkflowDefinitionStatusEnum.DISABLED.getId(), WorkflowDefinitionStatusEnum.PUBLISHED.getId()),
                updateCaptor.getAllValues().stream().map(WorkflowDefinition::getStatus).toList());
        verify(workflowNodeApproverDeptExpandService).rebuildByApproverIds(List.of(2001L));
        assertEquals(200L, result.getId());
    }

    @Test
    void shouldCreateNewPublishedVersionWhenUpdatingDisabledDefinition() {
        WorkflowDefinitionUpdateETO eto = new WorkflowDefinitionUpdateETO();
        eto.setId(12L);
        eto.setCurrentUserId(9L);
        eto.setName("请假审批-已停用");
        eto.setDescription("停用版本修改");
        eto.setWorkFlowJson(buildWorkflowJson("2"));

        WorkflowDefinition disabledEntity = buildDefinition(12L, WorkflowDefinitionStatusEnum.DISABLED.getId(), "LEAVE_APPROVAL", 1, "old");
        WorkflowDefinition publishedEntity = buildDefinition(13L, WorkflowDefinitionStatusEnum.PUBLISHED.getId(), "LEAVE_APPROVAL", 2, "old-published");
        WorkflowDefinition newEntity = buildDefinition(201L, WorkflowDefinitionStatusEnum.PUBLISHED.getId(), "LEAVE_APPROVAL", 3, eto.getWorkFlowJson());

        when(workflowDefinitionMapper.selectById(12L)).thenReturn(disabledEntity);
        when(workflowDefinitionMapper.selectMaxVersionByCode("LEAVE_APPROVAL")).thenReturn(2);
        when(workflowDefinitionMapper.selectById(201L)).thenReturn(newEntity);
        when(workflowDefinitionMapper.selectList(any())).thenReturn(List.of(disabledEntity, publishedEntity));
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
        assertEquals(WorkflowDefinitionStatusEnum.PUBLISHED.getId(), insertCaptor.getValue().getStatus());

        ArgumentCaptor<WorkflowDefinition> updateCaptor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(workflowDefinitionMapper, times(2)).updateById(updateCaptor.capture());
        assertEquals(List.of(13L, 201L), updateCaptor.getAllValues().stream().map(WorkflowDefinition::getId).toList());
        assertEquals(List.of(WorkflowDefinitionStatusEnum.DISABLED.getId(), WorkflowDefinitionStatusEnum.PUBLISHED.getId()),
                updateCaptor.getAllValues().stream().map(WorkflowDefinition::getStatus).toList());
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

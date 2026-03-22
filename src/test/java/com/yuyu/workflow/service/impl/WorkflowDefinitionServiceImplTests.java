package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.WorkflowDefinitionStructMapper;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionCreateETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionDisableETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionNodeETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionPublishETO;
import com.yuyu.workflow.eto.workflow.WorkflowNodeApproverETO;
import com.yuyu.workflow.eto.workflow.WorkflowTransitionETO;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.mapper.WorkflowDefinitionMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.mapper.WorkflowTransitionMapper;
import com.yuyu.workflow.vo.workflow.WorkflowDefinitionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
    private WorkflowDefinitionStructMapper workflowDefinitionStructMapper;

    @InjectMocks
    private WorkflowDefinitionServiceImpl workflowDefinitionService;

    @Test
    void shouldCreateDefinitionAndResolveTransitionNodeIds() {
        WorkflowDefinitionCreateETO eto = new WorkflowDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setName("请假审批");
        eto.setCode("LEAVE_APPROVAL");
        eto.setBizCode("LEAVE");

        WorkflowDefinitionNodeETO start = new WorkflowDefinitionNodeETO();
        start.setCode("start");
        start.setName("开始");
        start.setNodeType("START");

        WorkflowDefinitionNodeETO approval = new WorkflowDefinitionNodeETO();
        approval.setCode("leader");
        approval.setName("主管审批");
        approval.setNodeType("APPROVAL");
        approval.setApproveMode("AND");
        WorkflowNodeApproverETO approver = new WorkflowNodeApproverETO();
        approver.setApproverType("ROLE");
        approver.setApproverValue("1");
        approval.setApproverList(List.of(approver));

        WorkflowDefinitionNodeETO end = new WorkflowDefinitionNodeETO();
        end.setCode("end");
        end.setName("结束");
        end.setNodeType("END");

        WorkflowTransitionETO t1 = new WorkflowTransitionETO();
        t1.setFromNodeCode("start");
        t1.setToNodeCode("leader");
        WorkflowTransitionETO t2 = new WorkflowTransitionETO();
        t2.setFromNodeCode("leader");
        t2.setToNodeCode("end");

        eto.setNodes(List.of(start, approval, end));
        eto.setTransitions(List.of(t1, t2));

        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setName("请假审批");
        entity.setCode("LEAVE_APPROVAL");

        when(workflowDefinitionMapper.selectOne(any())).thenReturn(null);
        when(workflowDefinitionMapper.selectMaxVersionByCode("LEAVE_APPROVAL")).thenReturn(2);
        when(workflowDefinitionStructMapper.toEntity(eto)).thenReturn(entity);
        when(workflowDefinitionMapper.selectById(100L)).thenReturn(entity);
        when(workflowDefinitionStructMapper.toTarget(entity)).thenReturn(new WorkflowDefinitionVO());
        when(workflowNodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(workflowTransitionMapper.selectList(any())).thenReturn(Collections.emptyList());

        when(workflowDefinitionStructMapper.toNodeEntity(start)).thenReturn(new WorkflowNode());
        when(workflowDefinitionStructMapper.toNodeEntity(approval)).thenReturn(new WorkflowNode());
        when(workflowDefinitionStructMapper.toNodeEntity(end)).thenReturn(new WorkflowNode());
        when(workflowDefinitionStructMapper.toApproverEntity(approver)).thenReturn(new WorkflowNodeApprover());
        when(workflowDefinitionStructMapper.toTransitionEntity(t1)).thenReturn(new WorkflowTransition());
        when(workflowDefinitionStructMapper.toTransitionEntity(t2)).thenReturn(new WorkflowTransition());

        doAnswer(invocation -> {
            WorkflowDefinition argument = invocation.getArgument(0);
            argument.setId(100L);
            return 1;
        }).when(workflowDefinitionMapper).insert(any(WorkflowDefinition.class));
        doAnswer(new NodeInsertAnswer()).when(workflowNodeMapper).insert(any(WorkflowNode.class));

        workflowDefinitionService.create(eto);

        ArgumentCaptor<WorkflowDefinition> definitionCaptor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(workflowDefinitionMapper).insert(definitionCaptor.capture());
        assertEquals(3, definitionCaptor.getValue().getVersion());
        assertEquals(WorkflowDefinitionStatusEnum.DRAFT.getId(), definitionCaptor.getValue().getStatus());
        assertEquals(9L, definitionCaptor.getValue().getCreatedBy());

        ArgumentCaptor<WorkflowTransition> transitionCaptor = ArgumentCaptor.forClass(WorkflowTransition.class);
        verify(workflowTransitionMapper, org.mockito.Mockito.times(2)).insert(transitionCaptor.capture());
        assertEquals(List.of(1001L, 1002L), transitionCaptor.getAllValues().stream().map(WorkflowTransition::getFromNodeId).toList());
        assertEquals(List.of(1002L, 1003L), transitionCaptor.getAllValues().stream().map(WorkflowTransition::getToNodeId).toList());
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
}

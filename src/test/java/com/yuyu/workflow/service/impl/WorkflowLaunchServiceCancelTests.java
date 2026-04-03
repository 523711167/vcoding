package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yuyu.workflow.common.enums.BizApplyStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowInstanceStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import com.yuyu.workflow.config.JacksonConfig;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowCancelETO;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.service.BizDefinitionService;
import com.yuyu.workflow.service.WorkflowApprovalRecordService;
import com.yuyu.workflow.service.WorkflowDefinitionService;
import com.yuyu.workflow.service.WorkflowInstanceService;
import com.yuyu.workflow.service.WorkflowLaunchService;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
import com.yuyu.workflow.service.WorkflowNodeInstanceService;
import com.yuyu.workflow.service.WorkflowNodeService;
import com.yuyu.workflow.service.WorkflowParallelScopeService;
import com.yuyu.workflow.service.WorkflowTransitionService;
import com.yuyu.workflow.struct.WorkflowLaunchStructMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作流发起服务取消流程测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowLaunchServiceCancelTests {

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

    private WorkflowLaunchService workflowLaunchService;

    @BeforeEach
    void setUp() {
        ObjectMapperUtils objectMapperUtils = new ObjectMapperUtils(new JacksonConfig().objectMapper());
        workflowLaunchService = new WorkflowLaunchServiceImpl(
                bizApplyService,
                workflowInstanceService,
                workflowNodeInstanceService,
                workflowNodeApproverInstanceService,
                workflowApprovalRecordService,
                null,
                null,
                null,
                null,
                null,
                null,
                objectMapperUtils,
                mock(BizDefinitionService.class),
                mock(WorkflowDefinitionService.class),
                mock(WorkflowParallelScopeService.class),
                mock(WorkflowLaunchStructMapper.class),
                mock(WorkflowNodeService.class),
                mock(WorkflowTransitionService.class)
        );
    }

    @Test
    void shouldCancelRunningWorkflowForApplicant() {
        WorkflowCancelETO eto = buildCancelEto(1001L, 2001L, "发起人主动取消");
        WorkflowInstance workflowInstance = buildWorkflowInstance(1001L, 3001L, 2001L, WorkflowInstanceStatusEnum.RUNNING.getCode());
        BizApply bizApply = buildBizApply(3001L);
        WorkflowNodeInstance currentNodeInstance = buildCurrentNodeInstance(4001L, 1001L);

        when(workflowInstanceService.getByIdOrThrow(1001L)).thenReturn(workflowInstance);
        when(bizApplyService.getByIdOrThrow(3001L)).thenReturn(bizApply);
        when(workflowNodeInstanceService.list(anyWrapper())).thenReturn(List.of(currentNodeInstance));

        workflowLaunchService.cancel(eto);

        verify(workflowInstanceService).updateWorkflowInstanceForCancel(1001L);
        verify(workflowNodeInstanceService).updateNodeInstanceForCancel(1001L);
        verify(workflowNodeApproverInstanceService).cancelPendingApproversForInstance(1001L);
        verify(workflowApprovalRecordService).recordForCancel(eto, currentNodeInstance);

        ArgumentCaptor<BizApply> bizApplyCaptor = ArgumentCaptor.forClass(BizApply.class);
        verify(bizApplyService).updateById(bizApplyCaptor.capture());
        assertEquals(3001L, bizApplyCaptor.getValue().getId());
        assertEquals(BizApplyStatusEnum.INITIATOR_CANCELED.getCode(), bizApplyCaptor.getValue().getBizStatus());
    }

    @Test
    void shouldThrowWhenWorkflowIsNotRunning() {
        WorkflowCancelETO eto = buildCancelEto(1001L, 2001L, "流程已结束");
        WorkflowInstance workflowInstance = buildWorkflowInstance(1001L, 3001L, 2001L, WorkflowInstanceStatusEnum.APPROVED.getCode());
        BizApply bizApply = buildBizApply(3001L);
        WorkflowNodeInstance currentNodeInstance = buildCurrentNodeInstance(4001L, 1001L);

        when(workflowInstanceService.getByIdOrThrow(1001L)).thenReturn(workflowInstance);
        when(bizApplyService.getByIdOrThrow(3001L)).thenReturn(bizApply);
        when(workflowNodeInstanceService.list(anyWrapper())).thenReturn(List.of(currentNodeInstance));

        BizException exception = assertThrows(BizException.class, () -> workflowLaunchService.cancel(eto));

        assertEquals("流程已结束，不能取消", exception.getMessage());
        verify(workflowInstanceService, never()).updateWorkflowInstanceForCancel(any());
    }

    @Test
    void shouldThrowWhenCurrentUserIsNotApplicant() {
        WorkflowCancelETO eto = buildCancelEto(1001L, 9999L, "越权取消");
        WorkflowInstance workflowInstance = buildWorkflowInstance(1001L, 3001L, 2001L, WorkflowInstanceStatusEnum.RUNNING.getCode());
        BizApply bizApply = buildBizApply(3001L);
        WorkflowNodeInstance currentNodeInstance = buildCurrentNodeInstance(4001L, 1001L);

        when(workflowInstanceService.getByIdOrThrow(1001L)).thenReturn(workflowInstance);
        when(bizApplyService.getByIdOrThrow(3001L)).thenReturn(bizApply);
        when(workflowNodeInstanceService.list(anyWrapper())).thenReturn(List.of(currentNodeInstance));

        BizException exception = assertThrows(BizException.class, () -> workflowLaunchService.cancel(eto));

        assertEquals("无权取消该流程", exception.getMessage());
        verify(workflowApprovalRecordService, never()).recordForCancel(any(), any());
        verify(bizApplyService, never()).updateById(any());
    }

    private WorkflowCancelETO buildCancelEto(Long instanceId, Long currentUserId, String comment) {
        WorkflowCancelETO eto = new WorkflowCancelETO();
        eto.setInstanceId(instanceId);
        eto.setCurrentUserId(currentUserId);
        eto.setCurrentUsername("测试用户");
        eto.setComment(comment);
        return eto;
    }

    private WorkflowInstance buildWorkflowInstance(Long instanceId, Long bizId, Long applicantId, String status) {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setId(instanceId);
        workflowInstance.setBizId(bizId);
        workflowInstance.setApplicantId(applicantId);
        workflowInstance.setStatus(status);
        return workflowInstance;
    }

    private BizApply buildBizApply(Long bizId) {
        BizApply bizApply = new BizApply();
        bizApply.setId(bizId);
        return bizApply;
    }

    private WorkflowNodeInstance buildCurrentNodeInstance(Long nodeInstanceId, Long instanceId) {
        WorkflowNodeInstance workflowNodeInstance = new WorkflowNodeInstance();
        workflowNodeInstance.setId(nodeInstanceId);
        workflowNodeInstance.setInstanceId(instanceId);
        workflowNodeInstance.setDefinitionNodeId(5001L);
        workflowNodeInstance.setDefinitionNodeName("部门审批");
        workflowNodeInstance.setDefinitionNodeType("APPROVAL");
        return workflowNodeInstance;
    }

    @SuppressWarnings("unchecked")
    private Wrapper<WorkflowNodeInstance> anyWrapper() {
        return any(Wrapper.class);
    }
}

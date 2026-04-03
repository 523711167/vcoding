package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.mapper.BizApplyMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.WorkflowApprovalRecordMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.mapper.WorkflowInstanceMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverInstanceMapper;
import com.yuyu.workflow.mapper.WorkflowNodeInstanceMapper;
import com.yuyu.workflow.service.BizDefinitionService;
import com.yuyu.workflow.service.UserService;
import com.yuyu.workflow.service.WorkflowDefinitionService;
import com.yuyu.workflow.service.WorkflowParallelScopeService;
import com.yuyu.workflow.struct.BizApplyStructMapper;
import com.yuyu.workflow.struct.WorkflowApprovalRecordStructMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作流运行层基础服务测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowRuntimeServiceImplTests {

    @Mock
    private BizApplyMapper bizApplyMapper;

    @Mock
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Mock
    private WorkflowNodeInstanceMapper workflowNodeInstanceMapper;

    @Mock
    private WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper;

    @Mock
    private WorkflowApprovalRecordMapper workflowApprovalRecordMapper;
    @Mock
    private WorkflowNodeMapper workflowNodeMapper;
    @Mock
    private UserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private BizDefinitionService bizDefinitionService;
    @Mock
    private WorkflowDefinitionService workflowDefinitionService;
    @Mock
    private BizApplyStructMapper bizApplyStructMapper;
    @Mock
    private WorkflowParallelScopeService workflowParallelScopeService;
    @Mock
    private WorkflowApprovalRecordStructMapper workflowApprovalRecordStructMapper;

    private BizApplyServiceImpl bizApplyService;
    private WorkflowInstanceServiceImpl workflowInstanceService;
    private WorkflowNodeInstanceServiceImpl workflowNodeInstanceService;
    private WorkflowNodeApproverInstanceServiceImpl workflowNodeApproverInstanceService;
    private WorkflowApprovalRecordServiceImpl workflowApprovalRecordService;

    @BeforeEach
    void setUp() {
        bizApplyService = new BizApplyServiceImpl(
                bizApplyMapper,
                userMapper,
                bizDefinitionService,
                workflowDefinitionService,
                bizApplyStructMapper
        );
        workflowInstanceService = new WorkflowInstanceServiceImpl(workflowInstanceMapper);
        workflowNodeInstanceService = new WorkflowNodeInstanceServiceImpl(
                workflowNodeInstanceMapper,
                workflowParallelScopeService
        );
        workflowApprovalRecordService = new WorkflowApprovalRecordServiceImpl(
                workflowApprovalRecordMapper,
                workflowNodeMapper,
                workflowApprovalRecordStructMapper
        );
        workflowNodeApproverInstanceService = new WorkflowNodeApproverInstanceServiceImpl(
                workflowNodeApproverInstanceMapper,
                userService
        );
    }

    @Test
    void shouldSaveBizApply() {
        when(bizApplyMapper.insert(any(BizApply.class))).thenReturn(1);

        assertDoesNotThrow(() -> bizApplyService.save(new BizApply()));

        verify(bizApplyMapper).insert(any(BizApply.class));
    }

    @Test
    void shouldRejectUpdatingWorkflowInstanceWithoutId() {
        BizException exception = assertThrows(BizException.class,
                () -> workflowInstanceService.updateById(new WorkflowInstance()));

        assertEquals("流程实例id不能为空", exception.getMessage());
    }

    @Test
    void shouldBatchSaveNodeInstances() {
        when(workflowNodeInstanceMapper.insert(any(WorkflowNodeInstance.class))).thenReturn(1);

        workflowNodeInstanceService.saveBatch(List.of(new WorkflowNodeInstance(), new WorkflowNodeInstance()));

        verify(workflowNodeInstanceMapper, times(2)).insert(any(WorkflowNodeInstance.class));
    }

    @Test
    void shouldRejectUpdatingApproverInstanceWhenMapperAffectsNoRow() {
        WorkflowNodeApproverInstance entity = new WorkflowNodeApproverInstance();
        entity.setId(1L);
        when(workflowNodeApproverInstanceMapper.updateById(entity)).thenReturn(0);

        BizException exception = assertThrows(BizException.class,
                () -> workflowNodeApproverInstanceService.updateById(entity));

        assertEquals("节点审批人实例更新失败", exception.getMessage());
    }

    @Test
    void shouldBatchSaveApprovalRecords() {
        when(workflowApprovalRecordMapper.insert(any(WorkflowApprovalRecord.class))).thenReturn(1);

        workflowApprovalRecordService.saveBatch(List.of(new WorkflowApprovalRecord(), new WorkflowApprovalRecord()));

        verify(workflowApprovalRecordMapper, times(2)).insert(any(WorkflowApprovalRecord.class));
    }
}

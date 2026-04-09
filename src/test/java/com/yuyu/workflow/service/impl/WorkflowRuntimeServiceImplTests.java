package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowApprovalActionEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeApproverInstanceStatusEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowDelegateETO;
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
import org.mockito.ArgumentCaptor;
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

    @Test
    void shouldDelegateApproverTaskByUpdatingCurrentAndCreatingNewApprover() {
        WorkflowNodeApproverInstance currentApproverInstance = new WorkflowNodeApproverInstance();
        currentApproverInstance.setId(1L);
        currentApproverInstance.setNodeInstanceId(11L);
        currentApproverInstance.setInstanceId(21L);
        currentApproverInstance.setNodeName("直属领导审批");
        currentApproverInstance.setNodeType("APPROVAL");
        currentApproverInstance.setSortOrder(1);
        currentApproverInstance.setRelationType("ORIGINAL");
        currentApproverInstance.setStatus(WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode());
        currentApproverInstance.setIsActive(YesNoEnum.YES.getId());

        User delegateUser = new User();
        delegateUser.setId(9L);
        delegateUser.setUsername("zhangsan");
        delegateUser.setRealName("张三");
        delegateUser.setStatus(CommonStatusEnum.ENABLED.getId());

        when(workflowNodeApproverInstanceMapper.updateById(any(WorkflowNodeApproverInstance.class))).thenReturn(1);
        when(workflowNodeApproverInstanceMapper.insert(any(WorkflowNodeApproverInstance.class))).thenReturn(1);

        workflowNodeApproverInstanceService.saveApproverInstancesForDelegate(currentApproverInstance, delegateUser, "请帮忙处理");

        ArgumentCaptor<WorkflowNodeApproverInstance> updateCaptor = ArgumentCaptor.forClass(WorkflowNodeApproverInstance.class);
        ArgumentCaptor<WorkflowNodeApproverInstance> insertCaptor = ArgumentCaptor.forClass(WorkflowNodeApproverInstance.class);
        verify(workflowNodeApproverInstanceMapper).updateById(updateCaptor.capture());
        verify(workflowNodeApproverInstanceMapper).insert(insertCaptor.capture());

        WorkflowNodeApproverInstance updatedInstance = updateCaptor.getValue();
        assertEquals(WorkflowNodeApproverInstanceStatusEnum.DELEGATED.getCode(), updatedInstance.getStatus());
        assertEquals(YesNoEnum.NO.getId(), updatedInstance.getIsActive());
        assertEquals(9L, updatedInstance.getDelegateTo());
        assertEquals("张三", updatedInstance.getDelegateToName());
        assertEquals("请帮忙处理", updatedInstance.getComment());

        WorkflowNodeApproverInstance createdInstance = insertCaptor.getValue();
        assertEquals(11L, createdInstance.getNodeInstanceId());
        assertEquals(21L, createdInstance.getInstanceId());
        assertEquals(9L, createdInstance.getApproverId());
        assertEquals("张三", createdInstance.getApproverName());
        assertEquals(1, createdInstance.getSortOrder());
        assertEquals(WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode(), createdInstance.getStatus());
        assertEquals(YesNoEnum.YES.getId(), createdInstance.getIsActive());
    }

    @Test
    void shouldRecordDelegateApprovalAction() {
        WorkflowDelegateETO eto = new WorkflowDelegateETO();
        eto.setInstanceId(1001L);
        eto.setNodeInstanceId(2001L);
        eto.setApproverInstanceId(3001L);
        eto.setCurrentUserId(1L);
        eto.setCurrentUsername("admin");
        eto.setComment("转给张三处理");

        WorkflowNodeInstance workflowNodeInstance = new WorkflowNodeInstance();
        workflowNodeInstance.setDefinitionNodeId(88L);
        workflowNodeInstance.setDefinitionNodeName("直属领导审批");
        workflowNodeInstance.setDefinitionNodeType("APPROVAL");

        User delegateUser = new User();
        delegateUser.setId(9L);
        delegateUser.setUsername("zhangsan");
        delegateUser.setRealName("张三");

        when(workflowApprovalRecordMapper.insert(any(WorkflowApprovalRecord.class))).thenReturn(1);

        workflowApprovalRecordService.recordForDelegate(eto, workflowNodeInstance, delegateUser);

        ArgumentCaptor<WorkflowApprovalRecord> captor = ArgumentCaptor.forClass(WorkflowApprovalRecord.class);
        verify(workflowApprovalRecordMapper).insert(captor.capture());
        WorkflowApprovalRecord record = captor.getValue();
        assertEquals(WorkflowApprovalActionEnum.DELEGATE.getCode(), record.getAction());
        assertEquals(88L, record.getFromNodeId());
        assertEquals(88L, record.getToNodeId());
        assertEquals("转给张三处理", record.getComment());
        assertEquals("{\"delegateToUserId\":9,\"delegateToUsername\":\"zhangsan\",\"delegateToName\":\"张三\"}", record.getExtraData());
    }
}

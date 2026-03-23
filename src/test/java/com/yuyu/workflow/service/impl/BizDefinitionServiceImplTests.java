package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.BizDefinitionStructMapper;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.eto.biz.BizDefinitionCreateETO;
import com.yuyu.workflow.eto.biz.BizDefinitionUpdateETO;
import com.yuyu.workflow.mapper.BizDefinitionMapper;
import com.yuyu.workflow.mapper.WorkflowDefinitionMapper;
import com.yuyu.workflow.qto.biz.BizDefinitionListQTO;
import com.yuyu.workflow.service.BizDefinitionRoleRelService;
import com.yuyu.workflow.vo.biz.BizDefinitionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务定义服务测试。
 */
@ExtendWith(MockitoExtension.class)
class BizDefinitionServiceImplTests {

    @Mock
    private BizDefinitionMapper bizDefinitionMapper;

    @Mock
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Mock
    private BizDefinitionRoleRelService bizDefinitionRoleRelService;

    private BizDefinitionServiceImpl bizDefinitionService;

    @BeforeEach
    void setUp() {
        BizDefinitionStructMapper bizDefinitionStructMapper = Mappers.getMapper(BizDefinitionStructMapper.class);
        bizDefinitionService = new BizDefinitionServiceImpl(
                bizDefinitionMapper,
                workflowDefinitionMapper,
                bizDefinitionStructMapper,
                bizDefinitionRoleRelService
        );
    }

    /**
     * 新增业务定义时应写入创建人并绑定已发布流程。
     */
    @Test
    void shouldCreateBizDefinitionWithPublishedWorkflow() {
        BizDefinitionCreateETO eto = new BizDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setBizCode("LEAVE");
        eto.setBizName("请假申请");
        eto.setBizDesc("请假业务");
        eto.setWorkflowDefinitionId(100L);
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        WorkflowDefinition workflowDefinition = buildWorkflowDefinition(100L, "LEAVE_APPROVAL", "请假审批", WorkflowDefinitionStatusEnum.PUBLISHED.getId());
        BizDefinition savedEntity = buildBizDefinition(1L, "LEAVE", "请假申请", 100L, CommonStatusEnum.ENABLED.getId(), 9L);

        when(bizDefinitionMapper.selectAnyByBizCode("LEAVE")).thenReturn(null);
        when(workflowDefinitionMapper.selectById(100L)).thenReturn(workflowDefinition);
        when(bizDefinitionMapper.selectById(1L)).thenReturn(savedEntity);
        when(workflowDefinitionMapper.selectBatchIds(any())).thenReturn(List.of(workflowDefinition));
        doAnswer(invocation -> {
            BizDefinition entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        }).when(bizDefinitionMapper).insert(any(BizDefinition.class));

        BizDefinitionVO result = bizDefinitionService.create(eto);

        ArgumentCaptor<BizDefinition> captor = ArgumentCaptor.forClass(BizDefinition.class);
        verify(bizDefinitionMapper).insert(captor.capture());
        assertEquals("LEAVE", captor.getValue().getBizCode());
        assertEquals(9L, captor.getValue().getCreatedBy());
        assertEquals("请假审批", result.getWorkflowDefinitionName());
        assertEquals(CommonStatusEnum.ENABLED.getMsg(), result.getStatusMsg());
    }

    /**
     * 绑定的流程定义不是已发布状态时应拒绝。
     */
    @Test
    void shouldRejectBindingNonPublishedWorkflowDefinition() {
        BizDefinitionCreateETO eto = new BizDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setBizCode("LEAVE");
        eto.setBizName("请假申请");
        eto.setWorkflowDefinitionId(100L);
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        WorkflowDefinition workflowDefinition = buildWorkflowDefinition(100L, "LEAVE_APPROVAL", "请假审批", WorkflowDefinitionStatusEnum.DRAFT.getId());

        when(bizDefinitionMapper.selectAnyByBizCode("LEAVE")).thenReturn(null);
        when(workflowDefinitionMapper.selectById(100L)).thenReturn(workflowDefinition);

        BizException exception = assertThrows(BizException.class, () -> bizDefinitionService.create(eto));

        assertEquals("仅允许绑定已发布流程定义", exception.getMessage());
        verify(bizDefinitionMapper, never()).insert(any(BizDefinition.class));
    }

    /**
     * 修改业务定义时应保留原业务编码。
     */
    @Test
    void shouldKeepOriginalBizCodeWhenUpdating() {
        BizDefinitionUpdateETO eto = new BizDefinitionUpdateETO();
        eto.setId(1L);
        eto.setBizName("请假申请-新");
        eto.setBizDesc("新的描述");
        eto.setWorkflowDefinitionId(100L);
        eto.setStatus(CommonStatusEnum.DISABLED.getId());

        BizDefinition oldEntity = buildBizDefinition(1L, "LEAVE", "请假申请", 99L, CommonStatusEnum.ENABLED.getId(), 8L);
        WorkflowDefinition workflowDefinition = buildWorkflowDefinition(100L, "LEAVE_APPROVAL", "请假审批", WorkflowDefinitionStatusEnum.PUBLISHED.getId());
        BizDefinition updatedEntity = buildBizDefinition(1L, "LEAVE", "请假申请-新", 100L, CommonStatusEnum.DISABLED.getId(), 8L);
        updatedEntity.setBizDesc("新的描述");

        when(bizDefinitionMapper.selectById(1L)).thenReturn(oldEntity, updatedEntity);
        when(workflowDefinitionMapper.selectById(100L)).thenReturn(workflowDefinition);
        when(workflowDefinitionMapper.selectBatchIds(any())).thenReturn(List.of(workflowDefinition));

        BizDefinitionVO result = bizDefinitionService.update(eto);

        ArgumentCaptor<BizDefinition> captor = ArgumentCaptor.forClass(BizDefinition.class);
        verify(bizDefinitionMapper).updateById(captor.capture());
        assertEquals("LEAVE", captor.getValue().getBizCode());
        assertEquals(8L, captor.getValue().getCreatedBy());
        assertEquals(CommonStatusEnum.DISABLED.getId(), result.getStatus());
    }

    /**
     * 查询业务定义列表时应补充绑定流程定义信息。
     */
    @Test
    void shouldFillWorkflowDefinitionInfoWhenListing() {
        BizDefinitionListQTO qto = new BizDefinitionListQTO();
        qto.setBizCode("LEAVE");

        BizDefinition entity = buildBizDefinition(1L, "LEAVE", "请假申请", 100L, CommonStatusEnum.ENABLED.getId(), 9L);
        WorkflowDefinition workflowDefinition = buildWorkflowDefinition(100L, "LEAVE_APPROVAL", "请假审批", WorkflowDefinitionStatusEnum.PUBLISHED.getId());

        when(bizDefinitionMapper.selectList(any())).thenReturn(List.of(entity));
        when(workflowDefinitionMapper.selectBatchIds(any())).thenReturn(List.of(workflowDefinition));

        List<BizDefinitionVO> result = bizDefinitionService.list(qto);

        assertEquals(1, result.size());
        assertEquals("LEAVE_APPROVAL", result.get(0).getWorkflowDefinitionCode());
        assertEquals("请假审批", result.get(0).getWorkflowDefinitionName());
    }

    /**
     * 删除业务定义时应同步清理业务发起权限。
     */
    @Test
    void shouldRemoveInitiatorsWhenDeletingBizDefinition() {
        BizDefinition entity = buildBizDefinition(1L, "LEAVE", "请假申请", 100L, CommonStatusEnum.ENABLED.getId(), 9L);
        when(bizDefinitionMapper.selectById(1L)).thenReturn(entity);

        bizDefinitionService.delete(List.of(1L));

        verify(bizDefinitionRoleRelService).removeByBizDefinitionIds(List.of(1L));
        verify(bizDefinitionMapper).removeByIds(List.of(1L));
    }

    /**
     * 构造业务定义测试对象。
     */
    private BizDefinition buildBizDefinition(Long id,
                                             String bizCode,
                                             String bizName,
                                             Long workflowDefinitionId,
                                             Integer status,
                                             Long createdBy) {
        BizDefinition entity = new BizDefinition();
        entity.setId(id);
        entity.setBizCode(bizCode);
        entity.setBizName(bizName);
        entity.setWorkflowDefinitionId(workflowDefinitionId);
        entity.setStatus(status);
        entity.setCreatedBy(createdBy);
        return entity;
    }

    /**
     * 构造流程定义测试对象。
     */
    private WorkflowDefinition buildWorkflowDefinition(Long id, String code, String name, Integer status) {
        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setStatus(status);
        return entity;
    }
}

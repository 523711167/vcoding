package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.BizApplyStatusEnum;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.eto.biz.BizApplySaveDraftETO;
import com.yuyu.workflow.eto.biz.BizApplyUpdateDraftETO;
import com.yuyu.workflow.mapper.BizApplyMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.service.BizDefinitionService;
import com.yuyu.workflow.service.WorkflowDefinitionService;
import com.yuyu.workflow.struct.BizApplyCommandStructMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务申请服务测试。
 */
@ExtendWith(MockitoExtension.class)
class BizApplyServiceImplTests {

    @Mock
    private BizApplyMapper bizApplyMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BizDefinitionService bizDefinitionService;

    @Mock
    private WorkflowDefinitionService workflowDefinitionService;

    @Mock
    private BizApplyCommandStructMapper bizApplyCommandStructMapper;

    private BizApplyServiceImpl bizApplyService;

    @BeforeEach
    void setUp() {
        bizApplyService = new BizApplyServiceImpl(
                bizApplyMapper,
                userMapper,
                bizDefinitionService,
                workflowDefinitionService,
                bizApplyCommandStructMapper
        );
    }

    /**
     * 保存草稿时应冗余业务名称快照。
     */
    @Test
    void shouldSaveDraftWithBizNameSnapshot() {
        BizApplySaveDraftETO eto = buildSaveDraftETO();
        BizDefinition bizDefinition = buildBizDefinition(11L, "出差申请");

        when(bizDefinitionService.getById(11L)).thenReturn(bizDefinition);
        when(bizApplyMapper.insert(any(BizApply.class))).thenAnswer(invocation -> {
            BizApply bizApply = invocation.getArgument(0);
            bizApply.setId(100L);
            return 1;
        });

        BizApply saved = bizApplyService.saveDraft(eto);

        ArgumentCaptor<BizApply> captor = ArgumentCaptor.forClass(BizApply.class);
        verify(bizApplyMapper).insert(captor.capture());
        BizApply insertEntity = captor.getValue();
        assertEquals("出差申请", insertEntity.getBizName());
        assertEquals("出差申请", saved.getBizName());
        assertEquals(BizApplyStatusEnum.DRAFT.getCode(), insertEntity.getBizStatus());
    }

    /**
     * 修改草稿时应同步刷新业务名称快照。
     */
    @Test
    void shouldUpdateDraftWithBizNameSnapshot() {
        BizApplyUpdateDraftETO eto = buildUpdateDraftETO();
        BizApply current = new BizApply();
        current.setId(21L);
        current.setApplicantId(1001L);
        current.setBizStatus(BizApplyStatusEnum.DRAFT.getCode());
        current.setWorkflowInstanceId(null);

        BizApply latest = new BizApply();
        latest.setId(21L);
        latest.setBizDefinitionId(22L);
        latest.setBizName("费用报销");
        latest.setTitle("更新后标题");

        when(bizApplyMapper.selectById(21L)).thenReturn(current, latest);
        when(bizApplyMapper.updateById(any(BizApply.class))).thenReturn(1);
        when(bizDefinitionService.getById(22L)).thenReturn(buildBizDefinition(22L, "费用报销"));

        BizApply updated = bizApplyService.updateDraft(eto);

        ArgumentCaptor<BizApply> captor = ArgumentCaptor.forClass(BizApply.class);
        verify(bizApplyMapper).updateById(captor.capture());
        BizApply updateEntity = captor.getValue();
        assertEquals("费用报销", updateEntity.getBizName());
        assertEquals("费用报销", updated.getBizName());
    }

    /**
     * 构造保存草稿参数。
     */
    private BizApplySaveDraftETO buildSaveDraftETO() {
        BizApplySaveDraftETO eto = new BizApplySaveDraftETO();
        eto.setBizDefinitionId(11L);
        eto.setTitle("出差审批");
        eto.setFormData("{\"days\":2}");
        eto.setCurrentUserId(1001L);
        eto.setCurrentUsername("张三");
        eto.setCurrentPrimaryDeptId(2001L);
        return eto;
    }

    /**
     * 构造修改草稿参数。
     */
    private BizApplyUpdateDraftETO buildUpdateDraftETO() {
        BizApplyUpdateDraftETO eto = new BizApplyUpdateDraftETO();
        eto.setId(21L);
        eto.setBizDefinitionId(22L);
        eto.setTitle("更新后标题");
        eto.setFormData("{\"amount\":300}");
        eto.setCurrentUserId(1001L);
        eto.setCurrentUsername("张三");
        eto.setCurrentPrimaryDeptId(2001L);
        return eto;
    }

    /**
     * 构造业务定义对象。
     */
    private BizDefinition buildBizDefinition(Long id, String bizName) {
        BizDefinition bizDefinition = new BizDefinition();
        bizDefinition.setId(id);
        bizDefinition.setBizName(bizName);
        bizDefinition.setStatus(CommonStatusEnum.ENABLED.getId());
        return bizDefinition;
    }
}

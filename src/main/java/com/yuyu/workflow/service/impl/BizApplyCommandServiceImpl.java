package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.struct.BizApplyStructMapper;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.eto.biz.BizApplySaveAndSubmitETO;
import com.yuyu.workflow.service.BizApplyCommandService;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.service.WorkflowLaunchService;
import com.yuyu.workflow.vo.workflow.WorkflowBizSubmitVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务申请命令服务实现。
 */
@Service
public class BizApplyCommandServiceImpl implements BizApplyCommandService {

    private final BizApplyService bizApplyService;
    private final WorkflowLaunchService workflowLaunchService;
    private final BizApplyStructMapper bizApplyStructMapper;

    /**
     * 注入业务申请命令服务依赖。
     */
    public BizApplyCommandServiceImpl(BizApplyService bizApplyService,
                                      WorkflowLaunchService workflowLaunchService,
                                      BizApplyStructMapper bizApplyStructMapper) {
        this.bizApplyService = bizApplyService;
        this.workflowLaunchService = workflowLaunchService;
        this.bizApplyStructMapper = bizApplyStructMapper;
    }

    /**
     * 保存业务申请并立即提交审批。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBizSubmitVO saveAndSubmit(BizApplySaveAndSubmitETO eto) {
        BizApply bizApply = bizApplyService.saveDraft(bizApplyStructMapper.toBizApplySaveDraftETO(eto));

        workflowLaunchService.submit(bizApplyStructMapper.toWorkflowBizSubmitETO(eto, bizApply));

        BizApply latestBizApply = bizApplyService.getByIdOrThrow(bizApply.getId());
        WorkflowBizSubmitVO vo = new WorkflowBizSubmitVO();
        vo.setBizApplyId(latestBizApply.getId());
        vo.setWorkflowInstanceId(latestBizApply.getWorkflowInstanceId());
        return vo;
    }
}

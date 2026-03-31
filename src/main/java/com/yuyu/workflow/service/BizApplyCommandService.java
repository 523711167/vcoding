package com.yuyu.workflow.service;

import com.yuyu.workflow.eto.biz.BizApplySaveAndSubmitETO;
import com.yuyu.workflow.vo.workflow.WorkflowBizSubmitVO;

/**
 * 业务申请命令服务接口。
 */
public interface BizApplyCommandService {

    /**
     * 保存业务申请并立即提交审批。
     */
    WorkflowBizSubmitVO saveAndSubmit(BizApplySaveAndSubmitETO eto);
}

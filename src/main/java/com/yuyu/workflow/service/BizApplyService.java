package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.eto.biz.BizApplySaveDraftETO;
import com.yuyu.workflow.eto.biz.BizApplyUpdateDraftETO;
import com.yuyu.workflow.qto.biz.BizApplyDraftIdQTO;
import com.yuyu.workflow.qto.biz.BizApplyDraftListQTO;
import com.yuyu.workflow.qto.biz.BizApplyDraftPageQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryPageQTO;
import com.yuyu.workflow.vo.biz.BizApplyDraftVO;
import com.yuyu.workflow.vo.workflow.WorkflowQueryVO;

import java.util.List;

/**
 * 业务申请服务接口。
 */
public interface BizApplyService extends IService<BizApply> {

    /**
     * 按主键查询业务申请，不存在时抛出异常。
     */
    BizApply getByIdOrThrow(Long id);

    /**
     * 保存业务申请草稿。
     */
    BizApply saveDraft(BizApplySaveDraftETO eto);

    /**
     * 修改业务申请草稿。
     */
    BizApply updateDraft(BizApplyUpdateDraftETO eto);

    /**
     * 提交前校验业务申请是否允许发起。
     */
    void submitCheck(BizApply bizApply, Long currentUserId);

    /**
     * 按流程实例主键集合查询业务申请。
     */
    List<BizApply> listByWorkflowInstanceIds(List<Long> workflowInstanceIdList);

    /**
     * 查询当前用户草稿箱列表。
     */
    List<BizApplyDraftVO> listDrafts(BizApplyDraftListQTO qto);

    /**
     * 查询当前用户草稿箱详情。
     */
    BizApplyDraftVO detailDraft(BizApplyDraftIdQTO qto);

    /**
     * 分页查询当前用户草稿箱列表。
     */
    PageVo<BizApplyDraftVO> pageDrafts(BizApplyDraftPageQTO qto);

    /**
     * 查询查询箱列表。
     */
    List<WorkflowQueryVO> listQueries(WorkflowQueryListQTO qto);

    /**
     * 分页查询查询箱列表。
     */
    IPage<WorkflowQueryVO> pageQueries(IPage<WorkflowQueryVO> page, WorkflowQueryPageQTO qto);

    /**
     * 查询查询箱详情。
     */
    WorkflowQueryVO detailQuery(WorkflowQueryDetailQTO qto);

}

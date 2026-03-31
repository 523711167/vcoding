package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.struct.BizApplyCommandStructMapper;
import com.yuyu.workflow.eto.biz.BizApplySaveDraftETO;
import com.yuyu.workflow.eto.biz.BizApplySaveAndSubmitETO;
import com.yuyu.workflow.service.BizApplyCommandService;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.vo.biz.BizApplyDraftVO;
import com.yuyu.workflow.vo.workflow.WorkflowBizSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务申请控制器。
 */
@RestController
@Validated
@Tag(name = "业务申请")
@RequestMapping("/sys/biz-apply")
public class BizApplyController {

    private final BizApplyService bizApplyService;
    private final BizApplyCommandService bizApplyCommandService;
    private final BizApplyCommandStructMapper bizApplyCommandStructMapper;

    /**
     * 注入业务申请命令服务。
     */
    public BizApplyController(BizApplyService bizApplyService,
                              BizApplyCommandService bizApplyCommandService,
                              BizApplyCommandStructMapper bizApplyCommandStructMapper) {
        this.bizApplyService = bizApplyService;
        this.bizApplyCommandService = bizApplyCommandService;
        this.bizApplyCommandStructMapper = bizApplyCommandStructMapper;
    }

    /**
     * 保存业务申请草稿。
     */
    @Operation(summary = "保存业务申请草稿")
    @PostMapping("/save")
    public Resp<BizApplyDraftVO> save(@Valid @RequestBody BizApplySaveDraftETO eto) {
        return Resp.success(bizApplyCommandStructMapper.toBizApplyDraftVO(bizApplyService.saveDraft(eto)));
    }

    /**
     * 保存业务申请并立即提交审批。
     */
    @Operation(summary = "保存业务申请并立即提交审批")
    @PostMapping("/save-and-submit")
    public Resp<WorkflowBizSubmitVO> saveAndSubmit(@Valid @RequestBody BizApplySaveAndSubmitETO eto) {
        return Resp.success(bizApplyCommandService.saveAndSubmit(eto));
    }
}

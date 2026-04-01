package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.struct.BizApplyCommandStructMapper;
import com.yuyu.workflow.eto.biz.BizApplySaveDraftETO;
import com.yuyu.workflow.eto.biz.BizApplySaveAndSubmitETO;
import com.yuyu.workflow.eto.biz.BizApplyUpdateDraftETO;
import com.yuyu.workflow.qto.biz.BizApplyDraftIdQTO;
import com.yuyu.workflow.qto.biz.BizApplyDraftListQTO;
import com.yuyu.workflow.qto.biz.BizApplyDraftPageQTO;
import com.yuyu.workflow.service.BizApplyCommandService;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.vo.biz.BizApplyDraftVO;
import com.yuyu.workflow.vo.workflow.WorkflowBizSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * 修改业务申请草稿。
     */
    @Operation(summary = "修改业务申请草稿")
    @PostMapping("/update")
    public Resp<BizApplyDraftVO> update(@Valid @RequestBody BizApplyUpdateDraftETO eto) {
        return Resp.success(bizApplyCommandStructMapper.toBizApplyDraftVO(bizApplyService.updateDraft(eto)));
    }

    /**
     * 查询当前用户草稿箱列表。
     */
    @Operation(summary = "查询当前用户草稿箱列表")
    @GetMapping("/draft/list")
    public Resp<List<BizApplyDraftVO>> draftList(@Valid @ParameterObject BizApplyDraftListQTO qto) {
        return Resp.success(bizApplyService.listDrafts(qto));
    }

    /**
     * 分页查询当前用户草稿箱列表。
     */
    @Operation(summary = "分页查询当前用户草稿箱列表")
    @GetMapping("/draft/page")
    public Resp<PageVo<BizApplyDraftVO>> draftPage(@Valid @ParameterObject BizApplyDraftPageQTO qto) {
        return Resp.success(bizApplyService.pageDrafts(qto));
    }

    /**
     * 查询当前用户草稿箱详情。
     */
    @Operation(summary = "查询当前用户草稿箱详情")
    @GetMapping("/draft/detail")
    public Resp<BizApplyDraftVO> draftDetail(@Valid @ParameterObject BizApplyDraftIdQTO qto) {
        return Resp.success(bizApplyService.detailDraft(qto));
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

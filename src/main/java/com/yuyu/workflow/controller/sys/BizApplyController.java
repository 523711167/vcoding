package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.common.enums.BizApplyStatusEnum;
import com.yuyu.workflow.struct.BizApplyStructMapper;
import com.yuyu.workflow.eto.biz.BizApplySaveDraftETO;
import com.yuyu.workflow.eto.biz.BizApplySaveAndSubmitETO;
import com.yuyu.workflow.eto.biz.BizApplyUpdateDraftETO;
import com.yuyu.workflow.qto.biz.BizApplyDraftIdQTO;
import com.yuyu.workflow.qto.biz.BizApplyDraftListQTO;
import com.yuyu.workflow.qto.biz.BizApplyDraftPageQTO;
import com.yuyu.workflow.qto.biz.BizApplyLaunchIdQTO;
import com.yuyu.workflow.qto.biz.BizApplyLaunchListQTO;
import com.yuyu.workflow.qto.biz.BizApplyLaunchPageQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryPageQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoPageQTO;
import com.yuyu.workflow.service.BizApplyCommandService;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.service.WorkflowBizQueryService;
import com.yuyu.workflow.vo.biz.BizApplyDraftVO;
import com.yuyu.workflow.vo.workflow.WorkflowBizSubmitVO;
import com.yuyu.workflow.vo.workflow.WorkflowQueryVO;
import com.yuyu.workflow.vo.workflow.WorkflowTodoVO;
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
    private final WorkflowBizQueryService workflowBizQueryService;
    private final BizApplyStructMapper bizApplyStructMapper;

    /**
     * 注入业务申请命令服务。
     */
    public BizApplyController(BizApplyService bizApplyService,
                              BizApplyCommandService bizApplyCommandService,
                              WorkflowBizQueryService workflowBizQueryService,
                              BizApplyStructMapper bizApplyStructMapper) {
        this.bizApplyService = bizApplyService;
        this.bizApplyCommandService = bizApplyCommandService;
        this.workflowBizQueryService = workflowBizQueryService;
        this.bizApplyStructMapper = bizApplyStructMapper;
    }

    /**
     * 保存业务申请草稿。
     */
    @Operation(summary = "保存业务申请草稿")
    @PostMapping("/draft/save")
    public Resp<BizApplyDraftVO> save(@Valid @RequestBody BizApplySaveDraftETO eto) {
        return Resp.success(bizApplyStructMapper.toBizApplyDraftVO(bizApplyService.saveDraft(eto)));
    }

    /**
     * 修改业务申请草稿。
     */
    @Operation(summary = "修改业务申请草稿")
    @PostMapping("/draft/update")
    public Resp<BizApplyDraftVO> update(@Valid @RequestBody BizApplyUpdateDraftETO eto) {
        return Resp.success(bizApplyStructMapper.toBizApplyDraftVO(bizApplyService.updateDraft(eto)));
    }

    /**
     * 查询当前用户草稿箱列表。
     */
    @Operation(summary = "查询当前用户草稿箱列表")
    @GetMapping("/draft/list")
    public Resp<List<BizApplyDraftVO>> draftList(@Valid @ParameterObject BizApplyDraftListQTO qto) {
        qto.setBizStatusList(List.of(BizApplyStatusEnum.DRAFT.getCode()));
        return Resp.success(bizApplyService.listDrafts(qto));
    }

    /**
     * 分页查询当前用户草稿箱列表。
     */
    @Operation(summary = "分页查询当前用户草稿箱列表")
    @GetMapping("/draft/page")
    public Resp<PageVo<BizApplyDraftVO>> draftPage(@Valid @ParameterObject BizApplyDraftPageQTO qto) {
        qto.setBizStatusList(List.of(BizApplyStatusEnum.DRAFT.getCode()));
        return Resp.success(bizApplyService.pageDrafts(qto));
    }

    /**
     * 查询当前用户草稿箱详情。
     */
    @Operation(summary = "查询当前用户草稿箱详情")
    @GetMapping("/draft/detail")
    public Resp<BizApplyDraftVO> draftDetail(@Valid @ParameterObject BizApplyDraftIdQTO qto) {
        qto.setBizStatusList(List.of(BizApplyStatusEnum.DRAFT.getCode()));
        return Resp.success(bizApplyService.detailDraft(qto));
    }

    /**
     * 查询当前用户我的发起列表。
     */
    @Operation(summary = "查询当前用户我的发起列表")
    @GetMapping("/launch/list")
    public Resp<List<BizApplyDraftVO>> launchList(@Valid @ParameterObject BizApplyLaunchListQTO qto) {
        qto.setBizStatusList(List.of(
                BizApplyStatusEnum.PENDING.getCode(),
                BizApplyStatusEnum.APPROVED.getCode(),
                BizApplyStatusEnum.REJECTED.getCode(),
                BizApplyStatusEnum.CANCELED.getCode()
        ));
        return Resp.success(bizApplyService.listMineApplies(qto));
    }

    /**
     * 分页查询当前用户我的发起列表。
     */
    @Operation(summary = "分页查询当前用户我的发起列表")
    @GetMapping("/launch/page")
    public Resp<PageVo<BizApplyDraftVO>> launchPage(@Valid @ParameterObject BizApplyLaunchPageQTO qto) {
        qto.setBizStatusList(List.of(
                BizApplyStatusEnum.PENDING.getCode(),
                BizApplyStatusEnum.APPROVED.getCode(),
                BizApplyStatusEnum.REJECTED.getCode(),
                BizApplyStatusEnum.CANCELED.getCode()
        ));
        return Resp.success(bizApplyService.pageMineApplies(qto));
    }

    /**
     * 查询当前用户我的发起详情。
     */
    @Operation(summary = "查询当前用户我的发起详情")
    @GetMapping("/launch/detail")
    public Resp<BizApplyDraftVO> launchDetail(@Valid @ParameterObject BizApplyLaunchIdQTO qto) {
        qto.setBizStatusList(List.of(
                BizApplyStatusEnum.PENDING.getCode(),
                BizApplyStatusEnum.APPROVED.getCode(),
                BizApplyStatusEnum.REJECTED.getCode(),
                BizApplyStatusEnum.CANCELED.getCode()
        ));
        return Resp.success(bizApplyService.detailMineApply(qto));
    }

    /**
     * 查询当前用户代办箱列表。
     */
    @Operation(summary = "查询当前用户代办箱列表")
    @GetMapping("/todo/list")
    public Resp<List<WorkflowTodoVO>> todoList(@Valid @ParameterObject WorkflowTodoListQTO qto) {
        return Resp.success(workflowBizQueryService.todoList(qto));
    }

    /**
     * 分页查询当前用户代办箱列表。
     */
    @Operation(summary = "分页查询当前用户代办箱列表")
    @GetMapping("/todo/page")
    public Resp<PageVo<WorkflowTodoVO>> todoPage(@Valid @ParameterObject WorkflowTodoPageQTO qto) {
        return Resp.success(workflowBizQueryService.todoPage(qto));
    }

    /**
     * 查询当前用户代办箱详情。
     */
    @Operation(summary = "查询当前用户代办箱详情")
    @GetMapping("/todo/detail")
    public Resp<WorkflowTodoVO> todoDetail(@Valid @ParameterObject WorkflowTodoDetailQTO qto) {
        return Resp.success(workflowBizQueryService.todoDetail(qto));
    }

    /**
     * 查询当前用户查询箱列表。
     */
    @Operation(summary = "查询当前用户查询箱列表")
    @GetMapping("/query/list")
    public Resp<List<WorkflowQueryVO>> queryList(@Valid @ParameterObject WorkflowQueryListQTO qto) {
        return Resp.success(workflowBizQueryService.queryList(qto));
    }

    /**
     * 分页查询当前用户查询箱列表。
     */
    @Operation(summary = "分页查询当前用户查询箱列表")
    @GetMapping("/query/page")
    public Resp<PageVo<WorkflowQueryVO>> queryPage(@Valid @ParameterObject WorkflowQueryPageQTO qto) {
        return Resp.success(workflowBizQueryService.queryPage(qto));
    }

    /**
     * 查询当前用户查询箱详情。
     */
    @Operation(summary = "查询当前用户查询箱详情")
    @GetMapping("/query/detail")
    public Resp<WorkflowQueryVO> queryDetail(@Valid @ParameterObject WorkflowQueryDetailQTO qto) {
        return Resp.success(workflowBizQueryService.queryDetail(qto));
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

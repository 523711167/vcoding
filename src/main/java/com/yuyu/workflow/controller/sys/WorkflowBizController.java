package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.eto.workflow.WorkflowAddSignETO;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.eto.workflow.WorkflowBizSubmitETO;
import com.yuyu.workflow.eto.workflow.WorkflowDelegateETO;
import com.yuyu.workflow.eto.workflow.WorkflowRecallETO;
import com.yuyu.workflow.eto.workflow.WorkflowTimeoutHandleETO;
import com.yuyu.workflow.security.SecurityUtils;
import com.yuyu.workflow.service.WorkflowLaunchService;
import com.yuyu.workflow.service.model.workflow.WorkflowStartApproverResult;
import com.yuyu.workflow.service.model.workflow.WorkflowStartCommand;
import com.yuyu.workflow.service.model.workflow.WorkflowStartCurrentNodeResult;
import com.yuyu.workflow.service.model.workflow.WorkflowStartResult;
import com.yuyu.workflow.vo.workflow.WorkflowBizSubmitVO;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作流业务运行控制器。
 */
@RestController
@Validated
@Tag(name = "工作流业务运行")
@RequestMapping("/sys/workflow-biz")
public class WorkflowBizController {

    private final WorkflowLaunchService workflowLaunchService;

    /**
     * 注入工作流发起服务。
     */
    public WorkflowBizController(WorkflowLaunchService workflowLaunchService) {
        this.workflowLaunchService = workflowLaunchService;
    }

    /**
     * 提交业务申请并发起审批流程。
     */
    @Operation(summary = "提交业务申请并发起审批")
    @PostMapping("/submit")
    public Resp<WorkflowBizSubmitVO> submit(@Valid @RequestBody WorkflowBizSubmitETO eto) {
        WorkflowStartResult result = workflowLaunchService.startWorkflow(new WorkflowStartCommand(eto.getBizApplyId(), eto.getCurrentUserId()));
        WorkflowBizSubmitVO vo = new WorkflowBizSubmitVO();
        vo.setBizApplyId(result.bizApplyId());
        vo.setWorkflowInstanceId(result.workflowInstanceId());
        vo.setCurrentNode(toCurrentNodeVO(result.currentNode()));
        return Resp.success(vo);
    }

    /**
     * 审核当前节点，动作由 action 区分通过或拒绝。
     */
    @Operation(summary = "流程审核")
    @PostMapping("/audit")
    public Resp<Void> audit(@Valid @RequestBody WorkflowAuditETO eto) {
        workflowLaunchService.audit(eto);
        return Resp.success();
    }

    /**
     * 撤回运行中的流程实例。
     */
    @Operation(summary = "撤回流程")
    @PostMapping("/recall")
    public Resp<Void> recall(@Valid @RequestBody WorkflowRecallETO eto) {
        throw notImplemented("撤回流程");
    }

    /**
     * 将当前审批任务转交给其他用户。
     */
    @Operation(summary = "转交流程")
    @PostMapping("/delegate")
    public Resp<Void> delegate(@Valid @RequestBody WorkflowDelegateETO eto) {
        throw notImplemented("转交流程");
    }

    /**
     * 对当前审批任务发起加签。
     */
    @Operation(summary = "流程加签")
    @PostMapping("/add-sign")
    public Resp<Void> addSign(@Valid @RequestBody WorkflowAddSignETO eto) {
        throw notImplemented("流程加签");
    }

    /**
     * 预留给系统调度使用的节点超时处理入口。
     */
    @Hidden
    @PostMapping("/timeout/handle")
    public Resp<Void> handleTimeout(@Valid @RequestBody WorkflowTimeoutHandleETO eto) {
        throw notImplemented("节点超时处理");
    }

    /**
     * 转换当前运行节点视图对象。
     */
    private WorkflowBizSubmitVO.CurrentNodeVO toCurrentNodeVO(WorkflowStartCurrentNodeResult result) {
        if (result == null) {
            return null;
        }
        WorkflowBizSubmitVO.CurrentNodeVO vo = new WorkflowBizSubmitVO.CurrentNodeVO();
        vo.setNodeInstanceId(result.nodeInstanceId());
        vo.setNodeId(result.nodeId());
        vo.setNodeName(result.nodeName());
        vo.setNodeType(result.nodeType());
        vo.setStatus(result.status());
        vo.setApproveMode(result.approveMode());
        vo.setApproverList(result.approverList().stream()
                .map(this::toApproverVO)
                .toList());
        return vo;
    }

    /**
     * 转换当前节点审核人视图对象。
     */
    private WorkflowBizSubmitVO.ApproverVO toApproverVO(WorkflowStartApproverResult result) {
        WorkflowBizSubmitVO.ApproverVO vo = new WorkflowBizSubmitVO.ApproverVO();
        vo.setApproverId(result.approverId());
        vo.setApproverName(result.approverName());
        vo.setStatus(result.status());
        vo.setIsActive(result.isActive());
        vo.setSortOrder(result.sortOrder());
        vo.setRelationType(result.relationType());
        return vo;
    }

    /**
     * 统一返回骨架接口未实现提示。
     */
    private BizException notImplemented(String actionName) {
        return new BizException(actionName + "接口待实现");
    }
}

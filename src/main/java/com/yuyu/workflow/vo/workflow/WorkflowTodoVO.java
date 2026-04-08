package com.yuyu.workflow.vo.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代办箱返回对象。
 */
@Data
@Schema(description = "代办箱返回对象")
public class WorkflowTodoVO {

    @Schema(description = "审批人实例ID", example = "1001")
    private Long approverInstanceId;

    @Schema(description = "节点实例ID", example = "2001")
    private Long nodeInstanceId;

    @Schema(description = "流程实例ID", example = "3001")
    private Long workflowInstanceId;

    @Schema(description = "业务申请ID", example = "4001")
    private Long bizApplyId;

    @Schema(description = "业务定义ID", example = "1")
    private Long bizDefinitionId;

    @Schema(description = "业务名称", example = "费用报销")
    private String bizName;

    @Schema(description = "申请标题", example = "员工报销申请")
    private String title;

    @Schema(description = "申请人ID", example = "1")
    private Long applicantId;

    @Schema(description = "申请人姓名", example = "系统管理员")
    private String applicantName;

    @Schema(description = "业务表单JSON", example = "{\"amount\":1000}")
    private String formData;

    @Schema(description = "当前审批节点名称", example = "直属领导审批")
    private String nodeName;

    @Schema(description = "当前审批节点类型", example = "APPROVAL")
    private String nodeType;

    @Schema(description = "审批人实例状态", example = "PENDING")
    private String approverStatus;

    @Schema(description = "审批人实例状态说明", example = "待处理")
    private String approverStatusMsg;

    @Schema(description = "流程发起时间")
    private LocalDateTime startedAt;

    @Schema(description = "进入代办箱时间")
    private LocalDateTime todoAt;

    @Schema(description = "处理完成时间")
    private LocalDateTime processedAt;
}

package com.yuyu.workflow.vo.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询箱返回对象。
 */
@Data
@Schema(description = "查询箱返回对象")
public class WorkflowQueryVO {

    @Schema(description = "业务申请ID", example = "1001")
    private Long bizApplyId;

    @Schema(description = "业务定义ID", example = "1")
    private Long bizDefinitionId;

    @Schema(description = "业务名称", example = "费用报销")
    private String bizName;

    @Schema(description = "申请标题", example = "员工报销申请")
    private String title;

    @Schema(description = "业务申请状态", example = "APPROVED")
    private String bizStatus;

    @Schema(description = "业务申请状态说明", example = "已通过")
    private String bizStatusMsg;

    @Schema(description = "申请人ID", example = "1")
    private Long applicantId;

    @Schema(description = "申请人姓名", example = "系统管理员")
    private String applicantName;

    @Schema(description = "所属组织ID", example = "2")
    private Long deptId;

    @Schema(description = "流程实例ID", example = "3001")
    private Long workflowInstanceId;

    @Schema(description = "流程状态", example = "APPROVED")
    private String workflowStatus;

    @Schema(description = "流程状态说明", example = "已通过")
    private String workflowStatusMsg;

    @Schema(description = "当前节点名称", example = "直属领导审批")
    private String currentNodeName;

    @Schema(description = "当前节点类型", example = "APPROVAL")
    private String currentNodeType;

    @Schema(description = "业务表单JSON", example = "{\"amount\":1000}")
    private String formData;

    @Schema(description = "提交流程时间")
    private LocalDateTime submittedAt;

    @Schema(description = "流程结束时间")
    private LocalDateTime finishedAt;

    @Schema(description = "最近更新时间")
    private LocalDateTime updatedAt;
}

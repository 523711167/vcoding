package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.enums.WorkflowAuditActionEnum;
import com.yuyu.workflow.common.validation.EnumCodeValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流程审核参数。
 */
@Data
@Schema(description = "流程审核参数")
public class WorkflowRejectAuditETO {

    @NotNull(message = "instanceId不能为空")
    @Schema(description = "流程实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long instanceId;

    @NotNull(message = "nodeInstanceId不能为空")
    @Schema(description = "节点实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long nodeInstanceId;

    @NotNull(message = "approverInstanceId不能为空")
    @Schema(description = "审批人实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long approverInstanceId;

    @EnumCodeValid(enumClass = WorkflowAuditActionEnum.class, allowNull = false, message = "action不合法")
    @Schema(description = "审核动作：APPROVE/REJECT", requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVE")
    private String action;

    @Size(max = 500, message = "comment长度不能超过500")
    @Schema(description = "审核意见", example = "同意")
    private String comment;

    @Schema(description = "当前登录用户ID", hidden = true)
    private Long currentUserId;

    @Schema(description = "当前登录用户名", hidden = true)
    private String currentUsername;
}

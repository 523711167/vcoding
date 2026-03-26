package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程转交参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程转交参数")
public class WorkflowDelegateETO extends UserContextParam {

    @NotNull(message = "instanceId不能为空")
    @Schema(description = "流程实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long instanceId;

    @NotNull(message = "nodeInstanceId不能为空")
    @Schema(description = "节点实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long nodeInstanceId;

    @NotNull(message = "approverInstanceId不能为空")
    @Schema(description = "审批人实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long approverInstanceId;

    @NotNull(message = "delegateToUserId不能为空")
    @Schema(description = "转交目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    private Long delegateToUserId;

    @Size(max = 500, message = "comment长度不能超过500")
    @Schema(description = "转交说明", example = "请帮忙处理")
    private String comment;
}

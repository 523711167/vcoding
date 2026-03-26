package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程撤回参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程撤回参数")
public class WorkflowRecallETO extends UserContextParam {

    @NotNull(message = "instanceId不能为空")
    @Schema(description = "流程实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long instanceId;

    @Size(max = 500, message = "comment长度不能超过500")
    @Schema(description = "撤回原因", example = "填错了，撤回重提")
    private String comment;
}

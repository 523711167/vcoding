package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流程加签参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程加签参数")
public class WorkflowAddSignETO extends UserContextParam {

    @NotNull(message = "instanceId不能为空")
    @Schema(description = "流程实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long instanceId;

    @NotNull(message = "nodeInstanceId不能为空")
    @Schema(description = "节点实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long nodeInstanceId;

    @NotNull(message = "approverInstanceId不能为空")
    @Schema(description = "审批人实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long approverInstanceId;

    @NotEmpty(message = "addSignUserIds不能为空")
    @ArraySchema(schema = @Schema(description = "加签用户ID", example = "9"))
    private List<Long> addSignUserIds;

    @Size(max = 500, message = "comment长度不能超过500")
    @Schema(description = "加签说明", example = "请协同确认")
    private String comment;
}

package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点超时处理参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "节点超时处理参数")
public class WorkflowTimeoutHandleETO extends UserContextParam {

    @NotNull(message = "nodeInstanceId不能为空")
    @Schema(description = "节点实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long nodeInstanceId;
}

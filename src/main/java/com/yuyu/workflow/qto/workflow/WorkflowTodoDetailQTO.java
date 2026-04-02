package com.yuyu.workflow.qto.workflow;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代办箱详情查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代办箱详情查询参数")
public class WorkflowTodoDetailQTO extends BaseQueryQTO {

    @NotNull(message = "approverInstanceId不能为空")
    @Schema(description = "审批人实例ID", example = "1001")
    private Long approverInstanceId;
}

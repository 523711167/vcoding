package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.enums.WorkflowApproverTypeEnum;
import com.yuyu.workflow.common.validation.EnumCodeValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 节点审批人参数。
 */
@Data
@Schema(description = "节点审批人参数")
public class WorkflowNodeApproverETO {

    @Schema(description = "审批人类型：USER/ROLE/DEPT/INITIATOR_DEPT_LEADER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "approverType不能为空")
    @EnumCodeValid(enumClass = WorkflowApproverTypeEnum.class, allowNull = false, message = "approverType不合法")
    private String approverType;

    @Schema(description = "审批人值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "approverValue不能为空")
    @Size(max = 256, message = "approverValue长度不能超过256")
    private String approverValue;

    @Schema(description = "顺序值")
    private Integer sortOrder;
}

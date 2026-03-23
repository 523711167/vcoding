package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.enums.WorkflowApproveModeEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.common.enums.WorkflowTimeoutActionEnum;
import com.yuyu.workflow.common.validation.EnumCodeValid;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 工作流节点参数。
 */
@Data
@Schema(description = "工作流节点参数")
public class WorkflowNodeETO {

    @Schema(description = "节点编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "code不能为空")
    @Size(max = 64, message = "code长度不能超过64")
    private String code;

    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name不能为空")
    @Size(max = 100, message = "name长度不能超过100")
    private String name;

    @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "nodeType不能为空")
    @EnumCodeValid(enumClass = WorkflowNodeTypeEnum.class, allowNull = false, message = "nodeType不合法")
    private String nodeType;

    @Schema(description = "审批模式")
    @EnumCodeValid(enumClass = WorkflowApproveModeEnum.class, message = "approveMode不合法")
    private String approveMode;

    @Schema(description = "超时时长（分钟）")
    private Integer timeoutMinutes;

    @Schema(description = "超时处理策略")
    @EnumCodeValid(enumClass = WorkflowTimeoutActionEnum.class, message = "timeoutAction不合法")
    private String timeoutAction;

    @Schema(description = "提醒时长（分钟）")
    private Integer remindMinutes;

    @Schema(description = "节点X坐标")
    private Integer positionX;

    @Schema(description = "节点Y坐标")
    private Integer positionY;

    @Schema(description = "节点扩展配置JSON")
    private String configJson;

    @ArraySchema(schema = @Schema(implementation = WorkflowNodeApproverETO.class))
    @Valid
    private List<WorkflowNodeApproverETO> approverList;
}

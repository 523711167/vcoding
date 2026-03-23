package com.yuyu.workflow.vo.workflow;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程节点视图对象。
 */
@Data
@Schema(description = "流程节点视图对象")
public class WorkflowNodeVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "流程定义ID")
    private Long definitionId;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "节点类型说明")
    private String nodeTypeMsg;

    @Schema(description = "审批模式")
    private String approveMode;

    @Schema(description = "审批模式说明")
    private String approveModeMsg;

    @Schema(description = "超时时限（分钟）")
    private Integer timeoutMinutes;

    @Schema(description = "超时处理策略")
    private String timeoutAction;

    @Schema(description = "超时处理策略说明")
    private String timeoutActionMsg;

    @Schema(description = "提醒时限（分钟）")
    private Integer remindMinutes;

    @Schema(description = "画布X坐标")
    private Integer positionX;

    @Schema(description = "画布Y坐标")
    private Integer positionY;

    @Schema(description = "节点扩展配置JSON")
    private String configJson;

    @ArraySchema(schema = @Schema(implementation = WorkflowNodeApproverVO.class))
    private List<WorkflowNodeApproverVO> approverList = new ArrayList<>();
}

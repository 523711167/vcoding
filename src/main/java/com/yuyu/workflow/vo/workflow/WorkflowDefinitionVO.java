package com.yuyu.workflow.vo.workflow;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程定义视图对象。
 */
@Data
@Schema(description = "流程定义视图对象")
public class WorkflowDefinitionVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "流程编码")
    private String code;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "流程描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态说明")
    private String statusMsg;

    @Schema(description = "业务编码")
    private String bizCode;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @ArraySchema(schema = @Schema(implementation = WorkflowNodeVO.class))
    private List<WorkflowNodeVO> nodeList = new ArrayList<>();

    @ArraySchema(schema = @Schema(implementation = WorkflowTransitionVO.class))
    private List<WorkflowTransitionVO> transitionList = new ArrayList<>();
}

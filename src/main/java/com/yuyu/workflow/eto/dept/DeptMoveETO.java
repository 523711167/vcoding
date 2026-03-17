package com.yuyu.workflow.eto.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "移动部门参数")
public class DeptMoveETO {

    @Schema(description = "当前部门ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "目标父部门ID，顶级为0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "targetParentId不能为空")
    private Long targetParentId;
}

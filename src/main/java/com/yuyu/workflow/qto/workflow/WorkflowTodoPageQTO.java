package com.yuyu.workflow.qto.workflow;

import com.yuyu.workflow.qto.base.BasePageQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代办箱分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代办箱分页查询参数")
public class WorkflowTodoPageQTO extends BasePageQTO {

    @Schema(description = "业务申请ID", example = "1001")
    private Long bizApplyId;

    @Schema(description = "业务定义ID", example = "1")
    private Long bizDefinitionId;

    @Schema(description = "申请标题", example = "员工报销申请")
    private String title;
}

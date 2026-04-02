package com.yuyu.workflow.qto.workflow;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 查询箱列表查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询箱列表查询参数")
public class WorkflowQueryListQTO extends BaseQueryQTO {

    @Schema(description = "业务申请ID", example = "1001")
    private Long bizApplyId;

    @Schema(description = "业务定义ID", example = "1")
    private Long bizDefinitionId;

    @Schema(description = "申请标题", example = "员工报销申请")
    private String title;

    @Schema(description = "业务申请状态", example = "APPROVED")
    private String bizStatus;

    @Schema(hidden = true)
    private Boolean hasAllData;

    @Schema(hidden = true)
    private List<Long> visibleDeptIdList;
}

package com.yuyu.workflow.qto.workflow;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 查询箱详情查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询箱详情查询参数")
public class WorkflowQueryDetailQTO extends BaseQueryQTO {

    @NotNull(message = "bizApplyId不能为空")
    @Schema(description = "业务申请ID", example = "1001")
    private Long bizApplyId;

    @Schema(hidden = true)
    private Boolean hasAllData;

    @Schema(hidden = true)
    private List<Long> visibleDeptIdList;
}

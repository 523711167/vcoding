package com.yuyu.workflow.qto.biz;

import com.yuyu.workflow.qto.base.BasePageQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 当前用户业务申请分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前用户业务申请分页查询参数")
public class BizApplyMinePageQTO extends BasePageQTO {

    @Schema(description = "业务定义ID", example = "1")
    private Long bizDefinitionId;

    @Schema(description = "申请标题", example = "员工报销申请")
    private String title;

    @Schema(hidden = true)
    private List<String> bizStatusList;
}

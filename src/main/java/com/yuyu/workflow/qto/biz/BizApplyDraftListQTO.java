package com.yuyu.workflow.qto.biz;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务申请草稿列表查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务申请草稿列表查询参数")
public class BizApplyDraftListQTO extends BaseQueryQTO {

    @Schema(description = "业务定义ID", example = "1")
    private Long bizDefinitionId;

    @Schema(description = "申请标题", example = "员工报销申请")
    private String title;
}

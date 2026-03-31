package com.yuyu.workflow.eto.biz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改业务申请草稿参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "修改业务申请草稿参数")
public class BizApplyUpdateDraftETO extends BizApplySaveDraftETO {

    @NotNull(message = "id不能为空")
    @Schema(description = "业务申请ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}

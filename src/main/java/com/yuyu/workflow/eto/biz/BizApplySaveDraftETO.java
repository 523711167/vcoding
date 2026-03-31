package com.yuyu.workflow.eto.biz;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保存业务申请草稿参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "保存业务申请草稿参数")
public class BizApplySaveDraftETO extends UserContextParam {

    @NotNull(message = "bizDefinitionId不能为空")
    @Schema(description = "业务定义ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bizDefinitionId;

    @NotBlank(message = "title不能为空")
    @Size(max = 200, message = "title长度不能超过200")
    @Schema(description = "申请标题", example = "员工报销申请", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "formData不能为空")
    @Schema(description = "业务表单JSON", example = "{\"amount\":1000}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String formData;
}

package com.yuyu.workflow.eto.biz;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 业务绑定角色全量更新参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务绑定角色全量更新参数")
public class BizDefinitionRolesUpdateETO extends UserContextParam {

    @Schema(description = "业务定义ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @jakarta.validation.constraints.NotNull(message = "bizDefinitionId不能为空")
    private Long bizDefinitionId;

    @ArraySchema(schema = @Schema(description = "角色ID", example = "1"))
    private List<Long> roleIds;
}

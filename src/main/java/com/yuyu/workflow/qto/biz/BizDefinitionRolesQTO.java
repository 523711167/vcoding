package com.yuyu.workflow.qto.biz;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务绑定角色查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务绑定角色查询参数")
public class BizDefinitionRolesQTO extends BaseQueryQTO {

    @Schema(description = "业务定义ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "bizDefinitionId不能为空")
    private Long bizDefinitionId;
}

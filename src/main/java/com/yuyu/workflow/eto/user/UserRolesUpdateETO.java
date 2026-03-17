package com.yuyu.workflow.eto.user;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "全量更新用户角色参数")
public class UserRolesUpdateETO {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "userId不能为空")
    private Long userId;

    @ArraySchema(schema = @Schema(description = "角色ID"))
    private List<Long> roleIds;
}

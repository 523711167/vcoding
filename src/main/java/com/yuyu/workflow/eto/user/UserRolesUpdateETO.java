package com.yuyu.workflow.eto.user;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "全量更新用户角色参数")
public class UserRolesUpdateETO extends UserContextParam {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "userId不能为空")
    private Long userId;

    @ArraySchema(schema = @Schema(description = "角色ID"))
    private List<Long> roleIds;
}

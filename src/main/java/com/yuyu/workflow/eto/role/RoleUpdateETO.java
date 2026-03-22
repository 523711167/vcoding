package com.yuyu.workflow.eto.role;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "修改角色参数")
public class RoleUpdateETO extends UserContextParam {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name不能为空")
    @Size(max = 64, message = "name长度不能超过64")
    private String name;

    @Schema(description = "角色描述")
    @Size(max = 200, message = "description长度不能超过200")
    private String description;

    @Schema(description = "排序值")
    private Integer sortOrder;
}

package com.yuyu.workflow.vo.role;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "角色返回对象")
public class RoleVO {

    @Schema(description = "角色ID")
    private Long id;
    @Schema(description = "角色名称")
    private String name;
    @Schema(description = "角色编码")
    private String code;
    @Schema(description = "角色描述")
    private String description;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "状态说明")
    private String statusMsg;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "数据权限范围")
    private Integer dataScope;
    @Schema(description = "数据权限范围说明")
    private String dataScopeMsg;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @ArraySchema(schema = @Schema(description = "自定义部门ID"))
    private List<Long> customDeptIds;
}

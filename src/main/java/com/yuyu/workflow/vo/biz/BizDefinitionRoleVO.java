package com.yuyu.workflow.vo.biz;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 业务绑定角色返回对象。
 */
@Data
@Schema(description = "业务绑定角色返回对象")
public class BizDefinitionRoleVO {

    @Schema(description = "业务定义ID", example = "1")
    private Long bizDefinitionId;

    @ArraySchema(schema = @Schema(description = "已绑定角色ID", example = "1"))
    private List<Long> roleIds;
}

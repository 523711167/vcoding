package com.yuyu.workflow.qto.workflow;

import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程定义列表查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程定义列表查询参数")
public class WorkflowDefinitionListQTO extends BaseQueryQTO {

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "流程编码")
    private String code;

    @Schema(description = "状态：0=草稿 1=已发布 2=已停用")
    @EnumIdValid(enumClass = WorkflowDefinitionStatusEnum.class, message = "status不合法")
    private Integer status;
}

package com.yuyu.workflow.qto.workflow;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程定义主键查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程定义主键查询参数")
public class WorkflowDefinitionIdQTO extends BaseQueryQTO {
}

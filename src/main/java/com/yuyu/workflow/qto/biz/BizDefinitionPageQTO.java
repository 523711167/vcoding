package com.yuyu.workflow.qto.biz;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import com.yuyu.workflow.qto.base.BasePageQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务定义分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务定义分页查询参数")
public class BizDefinitionPageQTO extends BasePageQTO {

    @Schema(description = "业务编码")
    private String bizCode;

    @Schema(description = "业务名称")
    private String bizName;

    @Schema(description = "绑定流程定义编码")
    private String workflowDefinitionCode;

    @Schema(description = "状态：1=正常 0=停用")
    @EnumIdValid(enumClass = CommonStatusEnum.class, message = "status不合法")
    private Integer status;
}

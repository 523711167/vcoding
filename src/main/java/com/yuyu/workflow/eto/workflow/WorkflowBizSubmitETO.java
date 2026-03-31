package com.yuyu.workflow.eto.workflow;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提交业务申请并发起审批参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "提交业务申请并发起审批参数")
public class WorkflowBizSubmitETO extends UserContextParam {

    @NotNull(message = "bizApplyId不能为空")
    @Schema(description = "业务申请ID", example = "1")
    private Long bizApplyId;

}


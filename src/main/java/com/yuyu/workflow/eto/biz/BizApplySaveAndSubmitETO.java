package com.yuyu.workflow.eto.biz;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保存业务申请并立即提交审批参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "保存业务申请并立即提交审批参数")
public class BizApplySaveAndSubmitETO extends BizApplySaveDraftETO {
}

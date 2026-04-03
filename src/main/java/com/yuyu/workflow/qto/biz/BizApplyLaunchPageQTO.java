package com.yuyu.workflow.qto.biz;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的发起分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "我的发起分页查询参数")
public class BizApplyLaunchPageQTO extends BizApplyMinePageQTO {
}

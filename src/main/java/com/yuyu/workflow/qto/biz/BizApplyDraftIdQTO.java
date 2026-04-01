package com.yuyu.workflow.qto.biz;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务申请草稿详情查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务申请草稿详情查询参数")
public class BizApplyDraftIdQTO extends BaseQueryQTO {
}

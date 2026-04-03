package com.yuyu.workflow.qto.biz;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 当前用户业务申请详情查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前用户业务申请详情查询参数")
public class BizApplyMineDetailQTO extends BaseQueryQTO {

    @Schema(hidden = true)
    private List<String> bizStatusList;
}

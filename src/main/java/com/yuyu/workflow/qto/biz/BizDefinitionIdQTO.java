package com.yuyu.workflow.qto.biz;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务定义主键查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务定义主键查询参数")
public class BizDefinitionIdQTO extends BaseQueryQTO {
}

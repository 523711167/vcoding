package com.yuyu.workflow.qto.menu;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单主键查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "菜单主键查询参数")
public class MenuIdQTO extends BaseQueryQTO {
}

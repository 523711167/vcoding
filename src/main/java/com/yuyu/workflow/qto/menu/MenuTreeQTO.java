package com.yuyu.workflow.qto.menu;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.MenuTypeEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 菜单树查询参数。
 */
@Data
@Schema(description = "菜单树查询参数")
public class MenuTreeQTO extends BaseQueryQTO {

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "菜单类型：1=目录 2=菜单 3=按钮")
    @EnumIdValid(enumClass = MenuTypeEnum.class, message = "type不合法")
    private Integer type;

    @Schema(description = "是否显示：1=显示 0=隐藏")
    @EnumIdValid(enumClass = YesNoEnum.class, message = "visible不合法")
    private Integer visible;

    @Schema(description = "状态：1=正常 0=停用")
    @EnumIdValid(enumClass = CommonStatusEnum.class, message = "status不合法")
    private Integer status;
}

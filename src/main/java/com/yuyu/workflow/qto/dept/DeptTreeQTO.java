package com.yuyu.workflow.qto.dept;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "部门树查询参数")
public class DeptTreeQTO extends BaseQueryQTO {

    @Schema(description = "部门名称")
    private String name;
    @Schema(description = "状态：1=正常 0=停用")
    @EnumIdValid(enumClass = CommonStatusEnum.class, message = "status不合法")
    private Integer status;
}

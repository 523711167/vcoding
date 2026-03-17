package com.yuyu.workflow.qto.user;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import com.yuyu.workflow.qto.base.BasePageQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户分页查询参数")
public class UserPageQTO extends BasePageQTO {

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "状态：1=正常 0=停用")
    @EnumIdValid(enumClass = CommonStatusEnum.class, message = "status不合法")
    private Integer status;
}

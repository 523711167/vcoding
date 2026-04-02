package com.yuyu.workflow.qto.auth;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录日志主键查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志主键查询参数")
public class LoginLogIdQTO extends BaseQueryQTO {
}

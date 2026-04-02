package com.yuyu.workflow.qto.auth;

import com.yuyu.workflow.common.enums.LoginResultEnum;
import com.yuyu.workflow.common.validation.EnumCodeValid;
import com.yuyu.workflow.qto.base.BasePageQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 登录日志分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志分页查询参数")
public class LoginLogPageQTO extends BasePageQTO {

    @Schema(description = "登录用户名", example = "admin")
    private String username;

    @Schema(description = "登录结果：SUCCESS/FAIL", example = "SUCCESS")
    @EnumCodeValid(enumClass = LoginResultEnum.class, message = "result不合法")
    private String result;

    @Schema(description = "登录时间起始（yyyy-MM-dd HH:mm:ss）", example = "2026-04-02 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginAtStart;

    @Schema(description = "登录时间结束（yyyy-MM-dd HH:mm:ss）", example = "2026-04-02 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginAtEnd;
}

package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.qto.auth.LoginLogIdQTO;
import com.yuyu.workflow.qto.auth.LoginLogListQTO;
import com.yuyu.workflow.qto.auth.LoginLogPageQTO;
import com.yuyu.workflow.service.LoginLogService;
import com.yuyu.workflow.vo.auth.LoginLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证控制器。
 */
@RestController
@Validated
@Tag(name = "认证管理")
@RequestMapping("/sys/auth")
public class AuthController {

    private final LoginLogService loginLogService;

    /**
     * 注入认证模块依赖。
     */
    public AuthController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /**
     * 查询登录日志列表。
     */
    @Operation(summary = "查询登录日志列表")
    @GetMapping("/login-log/list")
    public Resp<List<LoginLogVO>> loginLogList(@Valid @ParameterObject LoginLogListQTO qto) {
        return Resp.success(loginLogService.list(qto));
    }

    /**
     * 分页查询登录日志列表。
     */
    @Operation(summary = "分页查询登录日志列表")
    @GetMapping("/login-log/page")
    public Resp<PageVo<LoginLogVO>> loginLogPage(@Valid @ParameterObject LoginLogPageQTO qto) {
        return Resp.success(loginLogService.page(qto));
    }

    /**
     * 查询登录日志详情。
     */
    @Operation(summary = "查询登录日志详情")
    @GetMapping("/login-log/detail")
    public Resp<LoginLogVO> loginLogDetail(@Valid @ParameterObject LoginLogIdQTO qto) {
        return Resp.success(loginLogService.detail(qto.getId()));
    }

}

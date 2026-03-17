package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.service.AuthService;
import com.yuyu.workflow.vo.auth.AuthContextVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 */
@RestController
@Validated
@Tag(name = "认证管理")
@RequestMapping("/sys/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 注入认证服务。
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 获取当前登录用户信息。
     */
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current-user")
    public Resp<AuthContextVO> currentUser() {
        return Resp.success(authService.currentUser());
    }
}

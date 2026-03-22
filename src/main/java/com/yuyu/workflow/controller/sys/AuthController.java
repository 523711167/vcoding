package com.yuyu.workflow.controller.sys;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
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


}

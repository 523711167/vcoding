package com.yuyu.workflow.service;

import com.yuyu.workflow.eto.auth.LoginETO;
import com.yuyu.workflow.vo.auth.AuthContextVO;
import com.yuyu.workflow.vo.auth.LoginVO;

/**
 * 认证服务接口。
 */
public interface AuthService {

    /**
     * 执行用户登录并返回 Token。
     */
    LoginVO login(LoginETO eto);

    /**
     * 获取当前登录用户信息。
     */
    AuthContextVO currentUser();
}

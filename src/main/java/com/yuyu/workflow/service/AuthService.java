package com.yuyu.workflow.service;

import com.yuyu.workflow.vo.auth.AuthContextVO;

/**
 * 认证服务接口。
 */
public interface AuthService {

    /**
     * 获取当前登录用户信息。
     */
    AuthContextVO currentUser();
}

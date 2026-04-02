package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.entity.LoginLog;
import com.yuyu.workflow.qto.auth.LoginLogListQTO;
import com.yuyu.workflow.qto.auth.LoginLogPageQTO;
import com.yuyu.workflow.vo.auth.LoginLogVO;

import java.util.List;

/**
 * 登录日志服务。
 */
public interface LoginLogService extends IService<LoginLog> {

    /**
     * 记录登录成功日志。
     */
    void recordSuccess(Long userId, String username, String clientIp, String userAgent);

    /**
     * 记录登录失败日志。
     */
    void recordFailure(String username, String failReason, String clientIp, String userAgent);

    /**
     * 查询登录日志列表。
     */
    List<LoginLogVO> list(LoginLogListQTO qto);

    /**
     * 分页查询登录日志列表。
     */
    PageVo<LoginLogVO> page(LoginLogPageQTO qto);

    /**
     * 查询登录日志详情。
     */
    LoginLogVO detail(Long id);
}

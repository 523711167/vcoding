package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户登录日志持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_login_log")
public class LoginLog extends BaseAuditEntity {

    /**
     * 登录用户ID，失败场景允许为空。
     */
    private Long userId;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 登录结果编码：SUCCESS/FAIL。
     */
    private String result;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 客户端IP地址。
     */
    private String clientIp;

    /**
     * 客户端User-Agent。
     */
    private String userAgent;

    /**
     * 登录发生时间。
     */
    private LocalDateTime loginAt;
}

package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user")
public class User extends BaseAuditEntity {

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 登录密码。
     */
    private String password;

    /**
     * 真实姓名。
     */
    private String realName;

    /**
     * 邮箱地址。
     */
    private String email;

    /**
     * 手机号。
     */
    private String mobile;

    /**
     * 头像地址。
     */
    private String avatar;

    /**
     * 状态值。
     */
    private Integer status;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginAt;
}

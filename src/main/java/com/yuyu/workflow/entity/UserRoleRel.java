package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色关联持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_role_rel")
public class UserRoleRel extends BaseCreateEntity {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 角色ID。
     */
    private Long roleId;
}

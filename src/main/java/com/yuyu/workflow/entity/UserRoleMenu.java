package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色菜单关联持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_role_menu")
public class UserRoleMenu extends BaseCreateEntity {

    /**
     * 角色ID。
     */
    private Long roleId;

    /**
     * 菜单ID。
     */
    private Long menuId;
}

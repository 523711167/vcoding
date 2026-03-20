package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色部门关联持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_role_dept")
public class UserRoleDept extends BaseCreateEntity {

    /**
     * 角色ID。
     */
    private Long roleId;

    /**
     * 部门ID。
     */
    private Long deptId;

    /**
     * 组织类型。
     */
    private String orgType;

    /**
     * 岗位类型。
     */
    private String postType;
}

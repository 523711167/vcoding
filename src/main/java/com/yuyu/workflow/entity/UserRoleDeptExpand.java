package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色数据权限组织展开关系持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_role_dept_expand")
public class UserRoleDeptExpand extends BaseCreateEntity {

    /**
     * 角色ID。
     */
    private Long roleId;

    /**
     * 来源直接绑定关系ID。
     */
    private Long sourceRelId;

    /**
     * 来源组织ID。
     */
    private Long sourceDeptId;

    /**
     * 来源组织类型。
     */
    private String sourceOrgType;

    /**
     * 来源岗位类型。
     */
    private String sourcePostType;

    /**
     * 目标组织ID。
     */
    private Long deptId;

    /**
     * 目标组织类型。
     */
    private String orgType;

    /**
     * 目标岗位类型。
     */
    private String postType;

    /**
     * 关系类型。
     */
    private String relationType;

    /**
     * 展开距离。
     */
    private Integer distance;
}

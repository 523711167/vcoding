package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户组织展开关系持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_dept_rel_expand")
public class UserDeptRelExpand extends BaseCreateEntity {

    /**
     * 用户ID。
     */
    private Long userId;

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
     * 是否来源于主组织绑定。
     */
    private Integer sourceIsPrimary;

    /**
     * 展开后的目标组织ID。
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

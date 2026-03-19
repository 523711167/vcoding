package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户部门关联持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_dept_rel")
public class UserDeptRel extends BaseCreateEntity {

    /**
     * 用户ID。
     */
    private Long userId;

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

    /**
     * 是否主部门。
     */
    private Integer isPrimary;
}

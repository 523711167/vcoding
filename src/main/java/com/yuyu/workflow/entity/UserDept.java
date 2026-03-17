package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_dept")
public class UserDept extends BaseAuditEntity {

    /**
     * 父部门ID。
     */
    private Long parentId;

    /**
     * 部门名称。
     */
    private String name;

    /**
     * 部门编码。
     */
    private String code;

    /**
     * 部门层级路径。
     */
    private String path;

    /**
     * 当前层级深度。
     */
    private Integer level;

    /**
     * 排序值。
     */
    private Integer sortOrder;

    /**
     * 部门主管用户ID。
     */
    private Long leaderId;

    /**
     * 部门主管姓名。
     */
    private String leaderName;

    /**
     * 状态值。
     */
    private Integer status;
}

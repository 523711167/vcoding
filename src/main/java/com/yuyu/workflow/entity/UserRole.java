package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_role")
public class UserRole extends BaseAuditEntity {

    /**
     * 角色名称。
     */
    private String name;

    /**
     * 角色编码。
     */
    private String code;

    /**
     * 角色描述。
     */
    private String description;

    /**
     * 状态值。
     */
    private Integer status;

    /**
     * 排序值。
     */
    private Integer sortOrder;

    /**
     * 数据权限范围。
     */
    private String dataScope;
}

package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_sys_menu")
public class SysMenu extends BaseAuditEntity {

    /**
     * 父节点ID。
     */
    private Long parentId;

    /**
     * 菜单类型。
     */
    private String type;

    /**
     * 菜单名称。
     */
    private String name;

    /**
     * 权限标识。
     */
    private String permission;

    /**
     * 路由地址。
     */
    private String path;

    /**
     * 前端组件路径。
     */
    private String component;

    /**
     * 图标标识。
     */
    private String icon;

    /**
     * 排序值。
     */
    private Integer sortOrder;

    /**
     * 是否可见。
     */
    private Integer visible;

    /**
     * 状态值。
     */
    private Integer status;
}

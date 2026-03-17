package com.yuyu.workflow.vo.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点返回对象。
 */
@Data
@Schema(description = "菜单树节点返回对象")
public class MenuTreeVO {

    @Schema(description = "菜单ID")
    private Long id;
    @Schema(description = "父节点ID")
    private Long parentId;
    @Schema(description = "菜单类型")
    private Integer type;
    @Schema(description = "菜单类型说明")
    private String typeMsg;
    @Schema(description = "菜单名称")
    private String name;
    @Schema(description = "权限标识")
    private String permission;
    @Schema(description = "路由地址")
    private String path;
    @Schema(description = "前端组件路径")
    private String component;
    @Schema(description = "图标标识")
    private String icon;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "是否显示")
    private Integer visible;
    @Schema(description = "是否显示说明")
    private String visibleMsg;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "状态说明")
    private String statusMsg;
    @Schema(description = "子节点")
    private List<MenuTreeVO> children = new ArrayList<>();
}

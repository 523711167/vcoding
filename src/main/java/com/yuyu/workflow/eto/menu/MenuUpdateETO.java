package com.yuyu.workflow.eto.menu;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.MenuTypeEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.validation.EnumCodeValid;
import com.yuyu.workflow.common.validation.EnumIdValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 修改菜单参数。
 */
@Data
@Schema(description = "修改菜单参数")
public class MenuUpdateETO {

    @Schema(description = "菜单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "父节点ID，顶级为0")
    private Long parentId;

    @Schema(description = "类型编码：DIRECTORY/MENU/BUTTON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "type不能为空")
    @EnumCodeValid(enumClass = MenuTypeEnum.class, allowNull = false, message = "type不合法")
    private String type;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name不能为空")
    @Size(max = 64, message = "name长度不能超过64")
    private String name;

    @Schema(description = "权限标识")
    @Size(max = 128, message = "permission长度不能超过128")
    private String permission;

    @Schema(description = "路由地址")
    @Size(max = 256, message = "path长度不能超过256")
    private String path;

    @Schema(description = "前端组件路径")
    @Size(max = 256, message = "component长度不能超过256")
    private String component;

    @Schema(description = "图标标识")
    @Size(max = 64, message = "icon长度不能超过64")
    private String icon;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "是否显示：1=显示 0=隐藏", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "visible不能为空")
    @EnumIdValid(enumClass = YesNoEnum.class, allowNull = false, message = "visible不合法")
    private Integer visible;

    @Schema(description = "状态：1=正常 0=停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "status不能为空")
    @EnumIdValid(enumClass = CommonStatusEnum.class, allowNull = false, message = "status不合法")
    private Integer status;

    /**
     * 按钮类型必须配置权限标识。
     */
    @AssertTrue(message = "按钮类型必须配置permission")
    public boolean isButtonPermissionValid() {
        return !MenuTypeEnum.BUTTON.getCode().equals(type) || StringUtils.hasText(permission);
    }

    /**
     * 按钮类型不允许配置路由地址。
     */
    @AssertTrue(message = "按钮类型不能配置path")
    public boolean isButtonPathValid() {
        return !MenuTypeEnum.BUTTON.getCode().equals(type) || !StringUtils.hasText(path);
    }

    /**
     * 按钮类型不允许配置前端组件路径。
     */
    @AssertTrue(message = "按钮类型不能配置component")
    public boolean isButtonComponentValid() {
        return !MenuTypeEnum.BUTTON.getCode().equals(type) || !StringUtils.hasText(component);
    }

    /**
     * 目录类型不允许配置前端组件路径。
     */
    @AssertTrue(message = "目录类型不能配置component")
    public boolean isDirectoryComponentValid() {
        return !MenuTypeEnum.DIRECTORY.getCode().equals(type) || !StringUtils.hasText(component);
    }
}

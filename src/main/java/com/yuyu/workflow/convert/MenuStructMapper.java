package com.yuyu.workflow.convert;

import com.yuyu.workflow.common.mapstruct.BaseMapper;
import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.eto.menu.MenuCreateETO;
import com.yuyu.workflow.eto.menu.MenuUpdateETO;
import com.yuyu.workflow.entity.SysMenu;
import com.yuyu.workflow.vo.menu.MenuTreeVO;
import com.yuyu.workflow.vo.menu.MenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 菜单对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface MenuStructMapper extends BaseMapper<SysMenu, MenuVO> {

    /**
     * 将菜单实体转换为菜单视图。
     */
    @Override
    @Mapping(target = "typeMsg", ignore = true)
    @Mapping(target = "visibleMsg", ignore = true)
    @Mapping(target = "statusMsg", ignore = true)
    MenuVO toTarget(SysMenu source);

    /**
     * 将新增菜单入参转换为菜单实体。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    SysMenu toEntity(MenuCreateETO eto);

    /**
     * 将修改菜单入参转换为新的菜单实体。
     */
    @Mapping(target = "id", source = "oldEntity.id")
    @Mapping(target = "parentId", source = "eto.parentId")
    @Mapping(target = "type", source = "eto.type")
    @Mapping(target = "name", source = "eto.name")
    @Mapping(target = "permission", source = "eto.permission")
    @Mapping(target = "path", source = "eto.path")
    @Mapping(target = "component", source = "eto.component")
    @Mapping(target = "icon", source = "eto.icon")
    @Mapping(target = "sortOrder", source = "eto.sortOrder")
    @Mapping(target = "visible", source = "eto.visible")
    @Mapping(target = "status", source = "eto.status")
    @Mapping(target = "createdAt", source = "oldEntity.createdAt")
    @Mapping(target = "updatedAt", source = "oldEntity.updatedAt")
    @Mapping(target = "isDeleted", ignore = true)
    SysMenu toUpdatedEntity(MenuUpdateETO eto, SysMenu oldEntity);

    /**
     * 将菜单实体转换为树节点对象。
     */
    @Mapping(target = "typeMsg", ignore = true)
    @Mapping(target = "visibleMsg", ignore = true)
    @Mapping(target = "statusMsg", ignore = true)
    @Mapping(target = "children", ignore = true)
    MenuTreeVO toTreeVO(SysMenu entity);
}

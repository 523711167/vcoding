package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.MenuTypeEnum;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.MenuStructMapper;
import com.yuyu.workflow.eto.menu.MenuCreateETO;
import com.yuyu.workflow.eto.menu.MenuUpdateETO;
import com.yuyu.workflow.entity.SysMenu;
import com.yuyu.workflow.entity.UserRoleMenu;
import com.yuyu.workflow.mapper.SysMenuMapper;
import com.yuyu.workflow.mapper.UserRoleMenuMapper;
import com.yuyu.workflow.qto.menu.MenuTreeQTO;
import com.yuyu.workflow.service.MenuService;
import com.yuyu.workflow.vo.menu.MenuTreeVO;
import com.yuyu.workflow.vo.menu.MenuVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 菜单服务实现。
 */
@Service
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper sysMenuMapper;
    private final UserRoleMenuMapper userRoleMenuMapper;
    private final MenuStructMapper menuStructMapper;

    /**
     * 注入菜单模块依赖组件。
     */
    public MenuServiceImpl(SysMenuMapper sysMenuMapper,
                           UserRoleMenuMapper userRoleMenuMapper,
                           MenuStructMapper menuStructMapper) {
        this.sysMenuMapper = sysMenuMapper;
        this.userRoleMenuMapper = userRoleMenuMapper;
        this.menuStructMapper = menuStructMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuVO create(MenuCreateETO eto) {
        Long parentId = normalizeParentId(eto.getParentId());
        SysMenu parent = getParentMenu(parentId);
        validateParentRelation(null, eto.getType(), parent);
        assertPermissionUnique(eto.getPermission(), null);

        SysMenu entity = menuStructMapper.toEntity(eto);
        entity.setParentId(parentId);
        entity.setSortOrder(Objects.isNull(eto.getSortOrder()) ? 0 : eto.getSortOrder());
        sysMenuMapper.insert(entity);
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuVO update(MenuUpdateETO eto) {
        SysMenu oldEntity = getMenuOrThrow(eto.getId());
        Long parentId = normalizeParentId(eto.getParentId());
        if (oldEntity.getId().equals(parentId)) {
            throw new BizException("菜单不能挂载到自身下");
        }
        SysMenu parent = getParentMenu(parentId);
        validateParentRelation(oldEntity.getId(), eto.getType(), parent);
        validateChildrenRelation(oldEntity.getId(), eto.getType());
        assertPermissionUnique(eto.getPermission(), oldEntity.getId());

        SysMenu entity = menuStructMapper.toUpdatedEntity(eto, oldEntity);
        entity.setParentId(parentId);
        entity.setSortOrder(Objects.isNull(eto.getSortOrder()) ? 0 : eto.getSortOrder());
        sysMenuMapper.updateById(entity);
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> idList) {
        List<Long> menuIds = normalizeDeleteIds(idList);
        for (Long menuId : menuIds) {
            getMenuOrThrow(menuId);
        }
        long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getParentId, menuIds)
                .notIn(SysMenu::getId, menuIds));
        if (childCount > 0) {
            throw new BizException("当前菜单存在未删除的子节点，无法删除");
        }
        deleteRoleMenuRelationsByMenuIds(menuIds);
        sysMenuMapper.deleteByIds(menuIds);
    }

    @Override
    public List<MenuTreeVO> tree(MenuTreeQTO qto) {
        List<SysMenu> menuList = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .like(StringUtils.hasText(qto.getName()), SysMenu::getName, qto.getName())
                .eq(Objects.nonNull(qto.getType()), SysMenu::getType, qto.getType())
                .eq(Objects.nonNull(qto.getVisible()), SysMenu::getVisible, qto.getVisible())
                .eq(Objects.nonNull(qto.getStatus()), SysMenu::getStatus, qto.getStatus())
                .orderByAsc(SysMenu::getSortOrder, SysMenu::getId));
        if (CollectionUtils.isEmpty(menuList)) {
            return Collections.emptyList();
        }
        Map<Long, MenuTreeVO> nodeMap = new LinkedHashMap<>();
        List<MenuTreeVO> rootList = new ArrayList<>();
        for (SysMenu menu : menuList) {
            MenuTreeVO node = menuStructMapper.toTreeVO(menu);
            fillTreeMeta(node, menu);
            nodeMap.put(node.getId(), node);
        }
        for (MenuTreeVO node : nodeMap.values()) {
            if (Objects.isNull(node.getParentId()) || node.getParentId() == 0L) {
                rootList.add(node);
                continue;
            }
            MenuTreeVO parent = nodeMap.get(node.getParentId());
            if (Objects.isNull(parent)) {
                rootList.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return rootList;
    }

    @Override
    public MenuVO detail(Long id) {
        SysMenu entity = getMenuOrThrow(id);
        MenuVO vo = menuStructMapper.toTarget(entity);
        fillDetailMeta(vo, entity);
        return vo;
    }

    /**
     * 按主键查询菜单，不存在时抛出业务异常。
     */
    private SysMenu getMenuOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        SysMenu menu = sysMenuMapper.selectById(id);
        if (Objects.isNull(menu)) {
            throw new BizException(RespCodeEnum.BIZ_ERROR.getId(), "菜单不存在");
        }
        return menu;
    }

    /**
     * 查询父菜单，顶级节点返回 null。
     */
    private SysMenu getParentMenu(Long parentId) {
        if (Objects.isNull(parentId) || parentId == 0L) {
            return null;
        }
        return getMenuOrThrow(parentId);
    }

    /**
     * 将空父节点统一转换为顶级节点主键。
     */
    private Long normalizeParentId(Long parentId) {
        return Objects.isNull(parentId) ? 0L : parentId;
    }

    /**
     * 校验当前菜单与父节点之间的类型关系是否合法。
     */
    private void validateParentRelation(Long selfId, Integer type, SysMenu parent) {
        if (MenuTypeEnum.BUTTON.getId().equals(type)) {
            if (Objects.isNull(parent) || !MenuTypeEnum.MENU.getId().equals(parent.getType())) {
                throw new BizException("按钮节点必须挂载在菜单节点下");
            }
        }
        if (MenuTypeEnum.MENU.getId().equals(type) && Objects.nonNull(parent)
                && !MenuTypeEnum.DIRECTORY.getId().equals(parent.getType())) {
            throw new BizException("菜单节点只能挂载在目录节点下");
        }
        if (MenuTypeEnum.DIRECTORY.getId().equals(type) && Objects.nonNull(parent)
                && !MenuTypeEnum.DIRECTORY.getId().equals(parent.getType())) {
            throw new BizException("目录节点只能挂载在目录节点下");
        }
        validateParentCycle(selfId, parent);
    }

    /**
     * 校验修改后的菜单类型是否仍与现有子节点兼容。
     */
    private void validateChildrenRelation(Long menuId, Integer type) {
        List<SysMenu> childList = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menuId));
        if (CollectionUtils.isEmpty(childList)) {
            return;
        }
        if (MenuTypeEnum.BUTTON.getId().equals(type)) {
            throw new BizException("按钮节点不能存在子节点");
        }
        if (MenuTypeEnum.MENU.getId().equals(type)) {
            boolean hasInvalidChild = childList.stream()
                    .anyMatch(child -> !MenuTypeEnum.BUTTON.getId().equals(child.getType()));
            if (hasInvalidChild) {
                throw new BizException("菜单节点下只允许挂载按钮节点");
            }
        }
        if (MenuTypeEnum.DIRECTORY.getId().equals(type)) {
            boolean hasInvalidChild = childList.stream()
                    .anyMatch(child -> MenuTypeEnum.BUTTON.getId().equals(child.getType()));
            if (hasInvalidChild) {
                throw new BizException("目录节点下不允许直接挂载按钮节点");
            }
        }
    }

    /**
     * 校验父节点不能是当前节点自身或其子孙节点。
     */
    private void validateParentCycle(Long selfId, SysMenu parent) {
        if (Objects.isNull(selfId) || Objects.isNull(parent)) {
            return;
        }
        SysMenu current = parent;
        while (Objects.nonNull(current)) {
            if (selfId.equals(current.getId())) {
                throw new BizException("目标父节点不能是当前菜单或其子节点");
            }
            current = getParentMenu(current.getParentId());
        }
    }

    /**
     * 校验权限标识全局唯一。
     */
    private void assertPermissionUnique(String permission, Long excludeId) {
        if (!StringUtils.hasText(permission)) {
            return;
        }
        SysMenu exist = sysMenuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPermission, permission)
                .ne(Objects.nonNull(excludeId), SysMenu::getId, excludeId)
                .last("limit 1"));
        if (Objects.nonNull(exist)) {
            throw new BizException("permission已存在");
        }
    }

    /**
     * 删除指定菜单关联的全部角色菜单关系。
     */
    private void deleteRoleMenuRelationsByMenuIds(List<Long> menuIds) {
        List<UserRoleMenu> relationList = userRoleMenuMapper.selectList(new LambdaQueryWrapper<UserRoleMenu>()
                .in(UserRoleMenu::getMenuId, menuIds));
        if (CollectionUtils.isEmpty(relationList)) {
            return;
        }
        userRoleMenuMapper.deleteByIds(relationList.stream().map(UserRoleMenu::getId).toList());
    }

    /**
     * 规范化删除主键集合，去空并去重，保证删除接口优先按批量语义执行。
     */
    private List<Long> normalizeDeleteIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            throw new BizException("idList不能为空");
        }
        List<Long> result = idList.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(result)) {
            throw new BizException("idList不能为空");
        }
        return result;
    }

    /**
     * 补充菜单详情对象中的枚举说明字段。
     */
    private void fillDetailMeta(MenuVO vo, SysMenu entity) {
        vo.setTypeMsg(MenuTypeEnum.getMsgById(entity.getType()));
        vo.setVisibleMsg(YesNoEnum.getMsgById(entity.getVisible()));
        vo.setStatusMsg(CommonStatusEnum.getMsgById(entity.getStatus()));
    }

    /**
     * 补充菜单树节点对象中的枚举说明字段。
     */
    private void fillTreeMeta(MenuTreeVO vo, SysMenu entity) {
        vo.setTypeMsg(MenuTypeEnum.getMsgById(entity.getType()));
        vo.setVisibleMsg(YesNoEnum.getMsgById(entity.getVisible()));
        vo.setStatusMsg(CommonStatusEnum.getMsgById(entity.getStatus()));
    }
}

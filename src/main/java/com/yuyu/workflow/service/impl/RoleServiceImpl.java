package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.DataScopeEnum;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.eto.role.RoleMenusUpdateETO;
import com.yuyu.workflow.convert.UserRoleStructMapper;
import com.yuyu.workflow.convert.UserStructMapper;
import com.yuyu.workflow.eto.role.RoleCreateETO;
import com.yuyu.workflow.eto.role.RoleDataScopeUpdateETO;
import com.yuyu.workflow.eto.role.RoleStatusUpdateETO;
import com.yuyu.workflow.eto.role.RoleUpdateETO;
import com.yuyu.workflow.entity.SysMenu;
import com.yuyu.workflow.entity.UserRoleRel;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.entity.UserRoleMenu;
import com.yuyu.workflow.entity.UserRoleDept;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.SysMenuMapper;
import com.yuyu.workflow.mapper.UserRoleMenuMapper;
import com.yuyu.workflow.mapper.UserRoleRelMapper;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserRoleDeptMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.qto.role.RoleListQTO;
import com.yuyu.workflow.qto.role.RolePageQTO;
import com.yuyu.workflow.service.RoleService;
import com.yuyu.workflow.vo.role.RoleMenuVO;
import com.yuyu.workflow.vo.role.RoleVO;
import com.yuyu.workflow.vo.role.UserSimpleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final UserRoleMapper userRoleMapper;
    private final UserRoleDeptMapper userRoleDeptMapper;
    private final UserRoleMenuMapper userRoleMenuMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final UserMapper userMapper;
    private final UserDeptMapper userDeptMapper;
    private final SysMenuMapper sysMenuMapper;
    private final UserRoleStructMapper userRoleStructMapper;
    private final UserStructMapper userStructMapper;

    /**
     * 注入角色模块依赖组件。
     */
    public RoleServiceImpl(UserRoleMapper userRoleMapper,
                           UserRoleDeptMapper userRoleDeptMapper,
                           UserRoleMenuMapper userRoleMenuMapper,
                           UserRoleRelMapper userRoleRelMapper,
                           UserMapper userMapper,
                           UserDeptMapper userDeptMapper,
                           SysMenuMapper sysMenuMapper,
                           UserRoleStructMapper userRoleStructMapper,
                           UserStructMapper userStructMapper) {
        this.userRoleMapper = userRoleMapper;
        this.userRoleDeptMapper = userRoleDeptMapper;
        this.userRoleMenuMapper = userRoleMenuMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.userMapper = userMapper;
        this.userDeptMapper = userDeptMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.userRoleStructMapper = userRoleStructMapper;
        this.userStructMapper = userStructMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO create(RoleCreateETO eto) {
        assertRoleCodeUnique(eto.getCode(), null);
        UserRole entity = userRoleStructMapper.toEntity(eto);
        entity.setStatus(CommonStatusEnum.ENABLED.getId());
        entity.setSortOrder(Objects.isNull(eto.getSortOrder()) ? 0 : eto.getSortOrder());
        entity.setDataScope(DataScopeEnum.ALL.getId());
        userRoleMapper.insert(entity);
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO update(RoleUpdateETO eto) {
        UserRole entity = userRoleStructMapper.toUpdatedEntity(eto, getRoleOrThrow(eto.getId()));
        entity.setSortOrder(Objects.isNull(eto.getSortOrder()) ? 0 : eto.getSortOrder());
        userRoleMapper.updateById(entity);
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> idList) {
        List<Long> roleIds = normalizeDeleteIds(idList);
        for (Long roleId : roleIds) {
            getRoleOrThrow(roleId);
        }
        long userCount = userRoleRelMapper.selectCount(new LambdaQueryWrapper<UserRoleRel>()
                .in(UserRoleRel::getRoleId, roleIds));
        if (userCount > 0) {
            throw new BizException("角色已关联用户，无法删除");
        }
        deleteRoleMenuRelations(roleIds);
        deleteRoleDeptRelations(roleIds);
        userRoleMapper.deleteByIds(roleIds);
    }

    @Override
    public List<RoleVO> list(RoleListQTO qto) {
        return buildRoleVOList(userRoleMapper.selectList(
                buildRoleQuery(qto.getName(), qto.getCode(), qto.getStatus())));
    }

    @Override
    public PageVo<RoleVO> page(RolePageQTO qto) {
        IPage<UserRole> page = userRoleMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(qto.getPageNum(), qto.getPageSize()),
                buildRoleQuery(qto.getName(), qto.getCode(), qto.getStatus())
        );
        return PageVo.of(page.getCurrent(), page.getSize(), page.getTotal(), buildRoleVOList(page.getRecords()));
    }

    @Override
    public RoleVO detail(Long id) {
        return buildRoleVO(getRoleOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(RoleStatusUpdateETO eto) {
        getRoleOrThrow(eto.getRoleId());
        userRoleMapper.update(null, new LambdaUpdateWrapper<UserRole>()
                .eq(UserRole::getId, eto.getRoleId())
                .set(UserRole::getStatus, eto.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenus(RoleMenusUpdateETO eto) {
        getRoleOrThrow(eto.getRoleId());
        List<Long> menuIds = expandRoleMenuIds(eto.getMenuIds());
        deleteRoleMenuRelations(List.of(eto.getRoleId()));
        for (Long menuId : menuIds) {
            UserRoleMenu relation = new UserRoleMenu();
            relation.setRoleId(eto.getRoleId());
            relation.setMenuId(menuId);
            userRoleMenuMapper.insert(relation);
        }
    }

    @Override
    public RoleMenuVO getMenus(Long roleId) {
        getRoleOrThrow(roleId);
        List<Long> menuIds = userRoleMenuMapper.selectList(new LambdaQueryWrapper<UserRoleMenu>()
                        .eq(UserRoleMenu::getRoleId, roleId))
                .stream()
                .map(UserRoleMenu::getMenuId)
                .distinct()
                .sorted()
                .toList();
        RoleMenuVO vo = new RoleMenuVO();
        vo.setRoleId(roleId);
        vo.setMenuIds(menuIds);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDataScope(RoleDataScopeUpdateETO eto) {
        UserRole role = getRoleOrThrow(eto.getRoleId());
        List<Long> deptIds = Objects.isNull(eto.getDeptIds()) ? Collections.emptyList() : eto.getDeptIds();
        if (DataScopeEnum.CUSTOM_DEPT.getId().equals(eto.getDataScope())) {
            if (CollectionUtils.isEmpty(deptIds)) {
                throw new BizException("自定义部门数据权限必须选择部门");
            }
            validateDeptIds(deptIds);
        }
        role.setDataScope(eto.getDataScope());
        userRoleMapper.updateById(role);
        deleteRoleDeptRelations(List.of(eto.getRoleId()));
        if (DataScopeEnum.CUSTOM_DEPT.getId().equals(eto.getDataScope())) {
            for (Long deptId : new LinkedHashSet<>(deptIds)) {
                UserRoleDept relation = new UserRoleDept();
                relation.setRoleId(eto.getRoleId());
                relation.setDeptId(deptId);
                userRoleDeptMapper.insert(relation);
            }
        }
    }

    @Override
    public List<UserSimpleVO> getUsers(Long roleId) {
        getRoleOrThrow(roleId);
        List<Long> userIds = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                        .eq(UserRoleRel::getRoleId, roleId))
                .stream()
                .map(UserRoleRel::getUserId)
                .toList();
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .map(userStructMapper::toUserSimpleVO)
                .toList();
    }

    /**
     * 按主键查询角色，不存在时抛出业务异常。
     */
    private UserRole getRoleOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        UserRole role = userRoleMapper.selectById(id);
        if (Objects.isNull(role)) {
            throw new BizException(RespCodeEnum.BIZ_ERROR.getId(), "角色不存在");
        }
        return role;
    }

    /**
     * 构造角色列表查询条件。
     */
    private LambdaQueryWrapper<UserRole> buildRoleQuery(String name, String code, Integer status) {
        return new LambdaQueryWrapper<UserRole>()
                .like(StringUtils.hasText(name), UserRole::getName, name)
                .like(StringUtils.hasText(code), UserRole::getCode, code)
                .eq(Objects.nonNull(status), UserRole::getStatus, status)
                .orderByAsc(UserRole::getSortOrder, UserRole::getId);
    }

    /**
     * 统一组装角色返回列表，保证 list/page 字段结构一致。
     */
    private List<RoleVO> buildRoleVOList(List<UserRole> roleList) {
        if (CollectionUtils.isEmpty(roleList)) {
            return Collections.emptyList();
        }
        return roleList.stream().map(this::buildRoleVO).toList();
    }

    /**
     * 校验角色编码全局唯一。
     */
    private void assertRoleCodeUnique(String code, Long excludeId) {
        UserRole exist = userRoleMapper.selectAnyByCode(code);
        if (Objects.nonNull(exist) && !exist.getId().equals(excludeId)) {
            throw new BizException("角色编码已存在");
        }
    }

    /**
     * 删除指定角色的全部部门关联数据。
     */
    private void deleteRoleDeptRelations(List<Long> roleIds) {
        List<UserRoleDept> relationList = userRoleDeptMapper.selectList(new LambdaQueryWrapper<UserRoleDept>()
                .in(UserRoleDept::getRoleId, roleIds));
        if (CollectionUtils.isEmpty(relationList)) {
            return;
        }
        userRoleDeptMapper.deleteByIds(relationList.stream().map(UserRoleDept::getId).toList());
    }

    /**
     * 删除指定角色的全部菜单关联数据。
     */
    private void deleteRoleMenuRelations(List<Long> roleIds) {
        List<UserRoleMenu> relationList = userRoleMenuMapper.selectList(new LambdaQueryWrapper<UserRoleMenu>()
                .in(UserRoleMenu::getRoleId, roleIds));
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
     * 校验自定义部门列表是否全部有效。
     */
    private void validateDeptIds(List<Long> deptIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>(deptIds);
        List<UserDept> deptList = userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>()
                .in(UserDept::getId, uniqueIds));
        if (deptList.size() != uniqueIds.size()) {
            throw new BizException("存在无效组织");
        }
    }

    /**
     * 校验菜单集合是否全部有效且启用。
     */
    private void validateMenuIds(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return;
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(menuIds);
        List<SysMenu> menuList = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, uniqueIds)
                .eq(SysMenu::getStatus, CommonStatusEnum.ENABLED.getId()));
        if (menuList.size() != uniqueIds.size()) {
            throw new BizException("存在无效菜单");
        }
    }

    /**
     * 规范化角色菜单授权主键集合，并自动补齐父节点与菜单下按钮节点。
     */
    private List<Long> expandRoleMenuIds(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return Collections.emptyList();
        }
        List<Long> normalizedIds = menuIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        validateMenuIds(normalizedIds);
        Map<Long, SysMenu> selectedMenuMap = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, normalizedIds)
                        .eq(SysMenu::getStatus, CommonStatusEnum.ENABLED.getId()))
                .stream()
                .collect(Collectors.toMap(SysMenu::getId, java.util.function.Function.identity()));
        LinkedHashSet<Long> result = new LinkedHashSet<>(normalizedIds);
        List<Long> menuNodeIds = selectedMenuMap.values().stream()
                .filter(item -> com.yuyu.workflow.common.enums.MenuTypeEnum.MENU.getId().equals(item.getType()))
                .map(SysMenu::getId)
                .toList();
        if (!CollectionUtils.isEmpty(menuNodeIds)) {
            sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                            .in(SysMenu::getParentId, menuNodeIds)
                            .eq(SysMenu::getStatus, CommonStatusEnum.ENABLED.getId()))
                    .stream()
                    .map(SysMenu::getId)
                    .forEach(result::add);
        }
        for (SysMenu menu : selectedMenuMap.values()) {
            SysMenu current = menu;
            while (Objects.nonNull(current.getParentId()) && current.getParentId() != 0L) {
                SysMenu parent = sysMenuMapper.selectById(current.getParentId());
                if (Objects.isNull(parent) || !CommonStatusEnum.ENABLED.getId().equals(parent.getStatus())) {
                    break;
                }
                result.add(parent.getId());
                current = parent;
            }
        }
        return result.stream().toList();
    }

    /**
     * 组装角色详情返回对象。
     */
    private RoleVO buildRoleVO(UserRole role) {
        RoleVO vo = userRoleStructMapper.toTarget(role);
        vo.setStatusMsg(CommonStatusEnum.getMsgById(role.getStatus()));
        vo.setDataScopeMsg(DataScopeEnum.getMsgById(role.getDataScope()));
        vo.setCustomDeptIds(userRoleDeptMapper.selectList(new LambdaQueryWrapper<UserRoleDept>()
                        .eq(UserRoleDept::getRoleId, role.getId()))
                .stream()
                .map(UserRoleDept::getDeptId)
                .collect(Collectors.toList()));
        return vo;
    }
}

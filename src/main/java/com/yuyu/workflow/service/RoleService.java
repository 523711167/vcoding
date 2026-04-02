package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.eto.role.RoleCreateETO;
import com.yuyu.workflow.eto.role.RoleDataScopeUpdateETO;
import com.yuyu.workflow.eto.role.RoleMenusUpdateETO;
import com.yuyu.workflow.eto.role.RoleStatusUpdateETO;
import com.yuyu.workflow.eto.role.RoleUpdateETO;
import com.yuyu.workflow.qto.role.RoleListQTO;
import com.yuyu.workflow.qto.role.RolePageQTO;
import com.yuyu.workflow.vo.role.RoleMenuVO;
import com.yuyu.workflow.vo.role.RoleVO;
import com.yuyu.workflow.vo.role.UserSimpleVO;

import java.util.List;

public interface RoleService extends IService<UserRole> {

    /**
     * 创建角色。
     */
    RoleVO create(RoleCreateETO eto);

    /**
     * 更新角色。
     */
    RoleVO update(RoleUpdateETO eto);

    /**
     * 按主键集合批量删除角色，单个删除视为批量删除的特例。
     */
    void delete(List<Long> idList);

    /**
     * 查询角色列表。
     */
    List<RoleVO> list(RoleListQTO qto);

    /**
     * 分页查询角色列表。
     */
    PageVo<RoleVO> page(RolePageQTO qto);

    /**
     * 查询角色详情。
     */
    RoleVO detail(Long id);

    /**
     * 更新角色状态。
     */
    void updateStatus(RoleStatusUpdateETO eto);

    /**
     * 全量更新角色菜单。
     */
    void updateMenus(RoleMenusUpdateETO eto);

    /**
     * 查询角色已关联菜单。
     */
    RoleMenuVO getMenus(Long roleId);

    /**
     * 更新角色数据权限范围。
     */
    void updateDataScope(RoleDataScopeUpdateETO eto);

    /**
     * 查询角色下用户。
     */
    List<UserSimpleVO> getUsers(Long roleId);

    /**
     * 查询指定用户已启用角色主键集合。
     */
    List<Long> listEnabledRoleIdsByUserId(Long userId);
}

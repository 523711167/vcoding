package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.MenuTypeEnum;
import com.yuyu.workflow.common.enums.RoleCodeEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.struct.MenuStructMapper;
import com.yuyu.workflow.eto.menu.MenuCreateETO;
import com.yuyu.workflow.entity.SysMenu;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.entity.UserRoleMenu;
import com.yuyu.workflow.mapper.SysMenuMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.mapper.UserRoleMenuMapper;
import com.yuyu.workflow.qto.menu.MenuTreeQTO;
import com.yuyu.workflow.vo.menu.MenuTreeVO;
import com.yuyu.workflow.vo.menu.MenuVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 菜单服务测试。
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceImplTests {

    @Mock
    private SysMenuMapper sysMenuMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserRoleMenuMapper userRoleMenuMapper;

    @Mock
    private MenuStructMapper menuStructMapper;

    @InjectMocks
    private MenuServiceImpl menuService;

    /**
     * 菜单详情应按字符串类型编码回填类型说明。
     */
    @Test
    void shouldFillTypeMsgByCodeWhenQueryingDetail() {
        SysMenu entity = new SysMenu();
        entity.setId(100L);
        entity.setType(MenuTypeEnum.MENU.getCode());
        entity.setVisible(YesNoEnum.YES.getId());
        entity.setStatus(CommonStatusEnum.ENABLED.getId());

        MenuVO menuVO = new MenuVO();
        menuVO.setId(100L);
        menuVO.setType(MenuTypeEnum.MENU.getCode());

        when(sysMenuMapper.selectById(100L)).thenReturn(entity);
        when(menuStructMapper.toTarget(entity)).thenReturn(menuVO);

        MenuVO result = menuService.detail(100L);

        assertEquals(MenuTypeEnum.MENU.getCode(), result.getType());
        assertEquals(MenuTypeEnum.MENU.getName(), result.getTypeMsg());
    }

    /**
     * 按钮节点只能挂载到菜单节点下。
     */
    @Test
    void shouldRejectButtonUnderDirectoryByCode() {
        MenuCreateETO eto = new MenuCreateETO();
        eto.setParentId(10L);
        eto.setType(MenuTypeEnum.BUTTON.getCode());
        eto.setName("新增按钮");
        eto.setPermission("sys:test:add");
        eto.setVisible(YesNoEnum.YES.getId());
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        SysMenu parent = new SysMenu();
        parent.setId(10L);
        parent.setType(MenuTypeEnum.DIRECTORY.getCode());

        when(sysMenuMapper.selectById(10L)).thenReturn(parent);

        BizException exception = assertThrows(BizException.class, () -> menuService.create(eto));

        assertEquals("按钮节点必须挂载在菜单节点下", exception.getMessage());
    }

    /**
     * 新增菜单后应自动授权给 ADMIN 角色。
     */
    @Test
    void shouldBindNewMenuToAdminRoleWhenCreatingMenu() {
        MenuCreateETO eto = new MenuCreateETO();
        eto.setParentId(0L);
        eto.setType(MenuTypeEnum.DIRECTORY.getCode());
        eto.setName("系统设置");
        eto.setVisible(YesNoEnum.YES.getId());
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        SysMenu entity = new SysMenu();
        entity.setId(88L);
        entity.setParentId(0L);
        entity.setType(MenuTypeEnum.DIRECTORY.getCode());
        entity.setVisible(YesNoEnum.YES.getId());
        entity.setStatus(CommonStatusEnum.ENABLED.getId());

        UserRole adminRole = new UserRole();
        adminRole.setId(1L);
        adminRole.setCode(RoleCodeEnum.ADMIN.getCode());

        MenuVO menuVO = new MenuVO();
        menuVO.setId(88L);
        menuVO.setType(MenuTypeEnum.DIRECTORY.getCode());

        when(menuStructMapper.toEntity(eto)).thenReturn(entity);
        when(userRoleMapper.selectAnyByCode(RoleCodeEnum.ADMIN.getCode())).thenReturn(adminRole);
        when(sysMenuMapper.selectById(88L)).thenReturn(entity);
        when(menuStructMapper.toTarget(entity)).thenReturn(menuVO);

        menuService.create(eto);

        org.mockito.ArgumentCaptor<UserRoleMenu> captor = org.mockito.ArgumentCaptor.forClass(UserRoleMenu.class);
        verify(userRoleMenuMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getRoleId());
        assertEquals(88L, captor.getValue().getMenuId());
    }

    /**
     * 查询菜单树时应按当前登录用户已授权菜单返回。
     */
    @Test
    void shouldQueryMenuTreeByCurrentUser() {
        MenuTreeQTO qto = new MenuTreeQTO();
        qto.setCurrentUserId(9L);
        qto.setCurrentUsername("admin");
        qto.setVisible(YesNoEnum.YES.getId());

        SysMenu root = new SysMenu();
        root.setId(1000L);
        root.setParentId(0L);
        root.setType(MenuTypeEnum.DIRECTORY.getCode());
        root.setName("系统管理");
        root.setVisible(YesNoEnum.YES.getId());
        root.setStatus(CommonStatusEnum.ENABLED.getId());

        SysMenu menu = new SysMenu();
        menu.setId(1100L);
        menu.setParentId(1000L);
        menu.setType(MenuTypeEnum.MENU.getCode());
        menu.setName("用户管理");
        menu.setVisible(YesNoEnum.YES.getId());
        menu.setStatus(CommonStatusEnum.ENABLED.getId());

        MenuTreeVO rootVO = new MenuTreeVO();
        rootVO.setId(1000L);
        rootVO.setParentId(0L);
        MenuTreeVO menuVO = new MenuTreeVO();
        menuVO.setId(1100L);
        menuVO.setParentId(1000L);

        when(sysMenuMapper.selectMenuTreeByUserId(9L, qto)).thenReturn(List.of(root, menu));
        when(menuStructMapper.toTreeVO(root)).thenReturn(rootVO);
        when(menuStructMapper.toTreeVO(menu)).thenReturn(menuVO);

        List<MenuTreeVO> result = menuService.tree(qto);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(1000L), result.get(0).getId());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals(Long.valueOf(1100L), result.get(0).getChildren().get(0).getId());
        verify(sysMenuMapper).selectMenuTreeByUserId(9L, qto);
    }
}

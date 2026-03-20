package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.MenuTypeEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.MenuStructMapper;
import com.yuyu.workflow.eto.menu.MenuCreateETO;
import com.yuyu.workflow.entity.SysMenu;
import com.yuyu.workflow.mapper.SysMenuMapper;
import com.yuyu.workflow.mapper.UserRoleMenuMapper;
import com.yuyu.workflow.vo.menu.MenuVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 菜单服务测试。
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceImplTests {

    @Mock
    private SysMenuMapper sysMenuMapper;

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
        assertEquals(MenuTypeEnum.MENU.getMsg(), result.getTypeMsg());
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
}

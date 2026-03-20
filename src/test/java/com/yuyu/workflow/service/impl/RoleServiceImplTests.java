package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.eto.role.RoleCreateETO;
import com.yuyu.workflow.eto.role.RoleDataScopeUpdateETO;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.mapper.SysMenuMapper;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.UserRoleDeptMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.mapper.UserRoleMenuMapper;
import com.yuyu.workflow.mapper.UserRoleRelMapper;
import com.yuyu.workflow.service.UserRoleDeptExpandService;
import com.yuyu.workflow.convert.UserRoleStructMapper;
import com.yuyu.workflow.convert.UserStructMapper;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserRoleDept;
import com.yuyu.workflow.vo.role.RoleVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色数据权限测试。
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTests {

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserRoleDeptMapper userRoleDeptMapper;

    @Mock
    private UserRoleMenuMapper userRoleMenuMapper;

    @Mock
    private UserRoleRelMapper userRoleRelMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserDeptMapper userDeptMapper;

    @Mock
    private SysMenuMapper sysMenuMapper;

    @Mock
    private UserRoleStructMapper userRoleStructMapper;

    @Mock
    private UserStructMapper userStructMapper;

    @Mock
    private UserRoleDeptExpandService userRoleDeptExpandService;

    @InjectMocks
    private RoleServiceImpl roleService;

    /**
     * 新增角色时默认数据权限应写入字符串编码。
     */
    @Test
    void shouldUseStringDataScopeWhenCreatingRole() {
        RoleCreateETO eto = new RoleCreateETO();
        eto.setName("财务");
        eto.setCode("FINANCE");

        UserRole entity = new UserRole();
        entity.setId(1L);
        entity.setName("财务");
        entity.setCode("FINANCE");
        entity.setStatus(CommonStatusEnum.ENABLED.getId());
        entity.setDataScope("ALL");

        when(userRoleStructMapper.toEntity(eto)).thenReturn(entity);
        when(userRoleStructMapper.toTarget(entity)).thenReturn(new RoleVO());
        when(userRoleMapper.selectAnyByCode("FINANCE")).thenReturn(null);
        when(userRoleMapper.selectById(1L)).thenReturn(entity);
        when(userRoleDeptMapper.selectList(any())).thenReturn(Collections.emptyList());

        roleService.create(eto);

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleMapper).insert(captor.capture());
        assertEquals("ALL", captor.getValue().getDataScope());
    }

    /**
     * 更新角色数据权限时应按字符串编码判断自定义部门模式。
     */
    @Test
    void shouldUpdateCustomDeptDataScopeByCode() {
        RoleDataScopeUpdateETO eto = new RoleDataScopeUpdateETO();
        eto.setRoleId(2L);
        eto.setDataScope("CUSTOM_DEPT");
        eto.setDeptIds(List.of(10L, 11L));

        UserRole role = new UserRole();
        role.setId(2L);
        role.setDataScope("ALL");

        UserDept dept1 = new UserDept();
        dept1.setId(10L);
        dept1.setOrgType("DEPT");
        UserDept dept2 = new UserDept();
        dept2.setId(11L);
        dept2.setOrgType("DEPT");

        when(userRoleMapper.selectById(2L)).thenReturn(role);
        when(userDeptMapper.selectList(any())).thenReturn(List.of(dept1, dept2));
        when(userRoleDeptMapper.selectList(any())).thenReturn(Collections.emptyList());

        roleService.updateDataScope(eto);

        assertEquals("CUSTOM_DEPT", role.getDataScope());
        verify(userRoleMapper).updateById(eq(role));
        ArgumentCaptor<UserRoleDept> captor = ArgumentCaptor.forClass(UserRoleDept.class);
        verify(userRoleDeptMapper, times(2)).insert(captor.capture());
        assertEquals(List.of(10L, 11L), captor.getAllValues().stream().map(UserRoleDept::getDeptId).toList());
        assertEquals(List.of("DEPT", "DEPT"), captor.getAllValues().stream().map(UserRoleDept::getOrgType).toList());
        verify(userRoleDeptExpandService).rebuildByRoleIds(List.of(2L));
    }
}

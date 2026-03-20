package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.convert.UserDeptStructMapper;
import com.yuyu.workflow.convert.UserRoleStructMapper;
import com.yuyu.workflow.convert.UserStructMapper;
import com.yuyu.workflow.eto.user.UserDeptItemETO;
import com.yuyu.workflow.eto.user.UserDeptsUpdateETO;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.mapper.UserRoleRelMapper;
import com.yuyu.workflow.service.UserDeptRelExpandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户组织关联测试。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserDeptMapper userDeptMapper;

    @Mock
    private UserRoleRelMapper userRoleRelMapper;

    @Mock
    private UserDeptRelMapper userDeptRelMapper;

    @Mock
    private UserStructMapper userStructMapper;

    @Mock
    private UserRoleStructMapper userRoleStructMapper;

    @Mock
    private UserDeptStructMapper userDeptStructMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDeptRelExpandService userDeptRelExpandService;

    @InjectMocks
    private UserServiceImpl userService;

    /**
     * 绑定岗位组织时应将岗位类型冗余写入关联表。
     */
    @Test
    void shouldPersistPostTypeWhenBindingPostDept() {
        UserDeptsUpdateETO eto = new UserDeptsUpdateETO();
        eto.setUserId(1L);

        UserDeptItemETO item = new UserDeptItemETO();
        item.setDeptId(10L);
        item.setIsPrimary(YesNoEnum.YES.getId());
        eto.setDepts(List.of(item));

        User user = new User();
        user.setId(1L);
        user.setStatus(CommonStatusEnum.ENABLED.getId());

        UserDept postDept = new UserDept();
        postDept.setId(10L);
        postDept.setOrgType("POST");
        postDept.setPostType("BACKEND_ENGINEER");
        postDept.setStatus(CommonStatusEnum.ENABLED.getId());

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userDeptMapper.selectList(any())).thenReturn(List.of(postDept));
        when(userDeptRelMapper.selectList(any())).thenReturn(Collections.emptyList());

        userService.updateDepts(eto);

        ArgumentCaptor<UserDeptRel> captor = ArgumentCaptor.forClass(UserDeptRel.class);
        verify(userDeptRelMapper).insert(captor.capture());
        assertEquals("POST", captor.getValue().getOrgType());
        assertEquals("BACKEND_ENGINEER", captor.getValue().getPostType());
        assertEquals(10L, captor.getValue().getDeptId());
        assertEquals(YesNoEnum.YES.getId(), captor.getValue().getIsPrimary());
        verify(userDeptRelExpandService).rebuildByUserIds(List.of(1L));
    }
}

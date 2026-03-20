package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.DataScopeEnum;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.entity.UserRoleDept;
import com.yuyu.workflow.entity.UserRoleDeptExpand;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserRoleDeptExpandMapper;
import com.yuyu.workflow.mapper.UserRoleDeptMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色数据权限组织展开关系重建测试。
 */
@ExtendWith(MockitoExtension.class)
class UserRoleDeptExpandServiceImplTests {

    @Mock
    private UserRoleDeptMapper userRoleDeptMapper;

    @Mock
    private UserRoleDeptExpandMapper userRoleDeptExpandMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserDeptMapper userDeptMapper;

    @InjectMocks
    private UserRoleDeptExpandServiceImpl userRoleDeptExpandService;

    /**
     * 重建展开关系时应写入自身和全部有效子孙节点。
     */
    @Test
    void shouldRebuildRoleDeptSelfAndDescendantRelations() {
        UserRole role = new UserRole();
        role.setId(2L);
        role.setDataScope(DataScopeEnum.CUSTOM_DEPT.getCode());

        UserRoleDept relation = new UserRoleDept();
        relation.setId(100L);
        relation.setRoleId(2L);
        relation.setDeptId(2L);
        relation.setOrgType("DEPT");

        UserDept sourceDept = buildDept(2L, 1L, "DEPT", null, "/1/2/");
        UserDept childDept = buildDept(3L, 2L, "DEPT", null, "/1/2/3/");
        UserDept postDept = buildDept(4L, 3L, "POST", "JAVA", "/1/2/3/4/");

        when(userRoleDeptExpandMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userRoleMapper.selectList(any())).thenReturn(List.of(role));
        when(userRoleDeptMapper.selectList(any())).thenReturn(List.of(relation));
        when(userDeptMapper.selectList(any())).thenReturn(
                List.of(sourceDept),
                List.of(sourceDept, childDept, postDept)
        );

        userRoleDeptExpandService.rebuildByRoleIds(List.of(2L));

        ArgumentCaptor<UserRoleDeptExpand> captor = ArgumentCaptor.forClass(UserRoleDeptExpand.class);
        verify(userRoleDeptExpandMapper, times(3)).insert(captor.capture());
        List<UserRoleDeptExpand> expandRelations = captor.getAllValues();

        assertEquals("SELF", expandRelations.get(0).getRelationType());
        assertEquals(0, expandRelations.get(0).getDistance());
        assertEquals(Long.valueOf(2L), expandRelations.get(0).getDeptId());

        assertEquals("DESCENDANT", expandRelations.get(1).getRelationType());
        assertEquals(1, expandRelations.get(1).getDistance());
        assertEquals(Long.valueOf(3L), expandRelations.get(1).getDeptId());

        assertEquals("DESCENDANT", expandRelations.get(2).getRelationType());
        assertEquals(2, expandRelations.get(2).getDistance());
        assertEquals(Long.valueOf(4L), expandRelations.get(2).getDeptId());
        assertEquals("JAVA", expandRelations.get(2).getPostType());
    }

    /**
     * 构造组织测试对象。
     */
    private UserDept buildDept(Long id, Long parentId, String orgType, String postType, String path) {
        UserDept dept = new UserDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setOrgType(orgType);
        dept.setPostType(postType);
        dept.setPath(path);
        dept.setStatus(CommonStatusEnum.ENABLED.getId());
        return dept;
    }
}

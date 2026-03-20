package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.entity.UserDeptRelExpand;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserDeptRelExpandMapper;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
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
 * 用户组织展开关系重建测试。
 */
@ExtendWith(MockitoExtension.class)
class UserDeptRelExpandServiceImplTests {

    @Mock
    private UserDeptRelMapper userDeptRelMapper;

    @Mock
    private UserDeptRelExpandMapper userDeptRelExpandMapper;

    @Mock
    private UserDeptMapper userDeptMapper;

    @InjectMocks
    private UserDeptRelExpandServiceImpl userDeptRelExpandService;

    /**
     * 重建展开关系时应同时写入自身和全部有效子孙节点。
     */
    @Test
    void shouldRebuildSelfAndDescendantRelations() {
        UserDeptRel sourceRel = new UserDeptRel();
        sourceRel.setId(100L);
        sourceRel.setUserId(1L);
        sourceRel.setDeptId(2L);
        sourceRel.setOrgType("DEPT");
        sourceRel.setIsPrimary(1);

        UserDept sourceDept = buildDept(2L, 1L, "DEPT", null, "/1/2/");
        UserDept childDept = buildDept(3L, 2L, "DEPT", null, "/1/2/3/");
        UserDept postDept = buildDept(4L, 3L, "POST", "BACKEND_ENGINEER", "/1/2/3/4/");

        when(userDeptRelExpandMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userDeptRelMapper.selectList(any())).thenReturn(List.of(sourceRel));
        when(userDeptMapper.selectList(any())).thenReturn(
                List.of(sourceDept),
                List.of(sourceDept, childDept, postDept)
        );

        userDeptRelExpandService.rebuildByUserIds(List.of(1L));

        ArgumentCaptor<UserDeptRelExpand> captor = ArgumentCaptor.forClass(UserDeptRelExpand.class);
        verify(userDeptRelExpandMapper, times(3)).insert(captor.capture());
        List<UserDeptRelExpand> savedRelations = captor.getAllValues();

        assertEquals("SELF", savedRelations.get(0).getRelationType());
        assertEquals(0, savedRelations.get(0).getDistance());
        assertEquals(Long.valueOf(2L), savedRelations.get(0).getDeptId());

        assertEquals("DESCENDANT", savedRelations.get(1).getRelationType());
        assertEquals(1, savedRelations.get(1).getDistance());
        assertEquals(Long.valueOf(3L), savedRelations.get(1).getDeptId());

        assertEquals("DESCENDANT", savedRelations.get(2).getRelationType());
        assertEquals(2, savedRelations.get(2).getDistance());
        assertEquals(Long.valueOf(4L), savedRelations.get(2).getDeptId());
        assertEquals("BACKEND_ENGINEER", savedRelations.get(2).getPostType());
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

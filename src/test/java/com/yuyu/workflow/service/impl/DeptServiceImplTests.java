package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.UserDeptStructMapper;
import com.yuyu.workflow.convert.UserStructMapper;
import com.yuyu.workflow.eto.dept.DeptCreateETO;
import com.yuyu.workflow.eto.dept.DeptMoveETO;
import com.yuyu.workflow.eto.dept.DeptUpdateETO;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserDeptRelExpand;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserDeptRelExpandMapper;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.service.UserDeptRelExpandService;
import com.yuyu.workflow.service.UserRoleDeptExpandService;
import com.yuyu.workflow.service.WorkflowNodeApproverDeptExpandService;
import com.yuyu.workflow.vo.dept.DeptVO;
import com.yuyu.workflow.vo.role.UserSimpleVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 组织层级规则测试。
 */
@ExtendWith(MockitoExtension.class)
class DeptServiceImplTests {

    @Mock
    private UserDeptMapper userDeptMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserDeptRelMapper userDeptRelMapper;

    @Mock
    private UserDeptRelExpandMapper userDeptRelExpandMapper;

    @Mock
    private UserDeptStructMapper userDeptStructMapper;

    @Mock
    private UserStructMapper userStructMapper;

    @Mock
    private UserDeptRelExpandService userDeptRelExpandService;

    @Mock
    private UserRoleDeptExpandService userRoleDeptExpandService;

    @Mock
    private WorkflowNodeApproverDeptExpandService workflowNodeApproverDeptExpandService;

    @InjectMocks
    private DeptServiceImpl deptService;

    /**
     * 顶级节点只能创建集团。
     */
    @Test
    void shouldRejectNonGroupRootCreation() {
        DeptCreateETO eto = new DeptCreateETO();
        eto.setParentId(0L);
        eto.setName("顶级岗位");
        eto.setOrgType("POST");
        eto.setPostType("MANAGER");
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        BizException exception = assertThrows(BizException.class, () -> deptService.create(eto));

        assertEquals("顶级组织只能是集团", exception.getMessage());
    }

    /**
     * 集团下只能新增公司。
     */
    @Test
    void shouldRejectCreatingDeptUnderGroup() {
        DeptCreateETO eto = new DeptCreateETO();
        eto.setParentId(1L);
        eto.setName("研发部");
        eto.setOrgType("DEPT");
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        when(userDeptMapper.selectById(1L)).thenReturn(buildDept(1L, 0L, "GROUP", null, "/1/", 1));

        BizException exception = assertThrows(BizException.class, () -> deptService.create(eto));

        assertEquals("集团下只能新增公司", exception.getMessage());
    }

    /**
     * 岗位节点下不允许挂接任何子组织。
     */
    @Test
    void shouldRejectMovingDeptUnderPost() {
        DeptMoveETO eto = new DeptMoveETO();
        eto.setId(2L);
        eto.setTargetParentId(3L);

        when(userDeptMapper.selectById(2L)).thenReturn(buildDept(2L, 1L, "DEPT", null, "/1/2/", 2));
        when(userDeptMapper.selectById(3L)).thenReturn(buildDept(3L, 9L, "POST", "MANAGER", "/9/3/", 2));

        BizException exception = assertThrows(BizException.class, () -> deptService.move(eto));

        assertEquals("岗位下不允许新增子组织", exception.getMessage());
    }

    /**
     * 岗位节点必须提供岗位类型。
     */
    @Test
    void shouldRequirePostTypeForPostOrg() {
        DeptCreateETO eto = new DeptCreateETO();
        eto.setParentId(2L);
        eto.setName("后端开发岗");
        eto.setOrgType("POST");
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        when(userDeptMapper.selectById(2L)).thenReturn(buildDept(2L, 1L, "DEPT", null, "/1/2/", 2));

        BizException exception = assertThrows(BizException.class, () -> deptService.create(eto));

        assertEquals("岗位类型不能为空", exception.getMessage());
    }

    /**
     * 更新组织时不允许变更组织类型，应保留原始 orgType。
     */
    @Test
    void shouldKeepOriginalOrgTypeWhenUpdatingDept() {
        DeptUpdateETO eto = new DeptUpdateETO();
        eto.setId(2L);
        eto.setName("研发中心");
        eto.setCode("RD");
        eto.setSortOrder(10);
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        UserDept oldDept = buildDept(2L, 1L, "DEPT", null, "/1/2/", 2);
        oldDept.setName("研发部");
        oldDept.setCode("DEV");

        UserDept updatedDept = buildDept(2L, 1L, "DEPT", null, "/1/2/", 2);
        updatedDept.setName("研发中心");
        updatedDept.setCode("RD");
        updatedDept.setSortOrder(10);

        when(userDeptMapper.selectById(2L)).thenReturn(oldDept, updatedDept);
        when(userDeptMapper.selectById(1L)).thenReturn(buildDept(1L, 0L, "COMPANY", null, "/1/", 1));
        when(userDeptStructMapper.toUpdatedEntity(eq(eto), eq(oldDept))).thenReturn(updatedDept);
        when(userDeptStructMapper.toTarget(updatedDept)).thenReturn(new DeptVO());

        deptService.update(eto);

        verify(userDeptMapper).updateById(argThat(dept -> "DEPT".equals(dept.getOrgType())));
    }

    /**
     * 非岗位节点更新时传入岗位类型仍应按现有规则拦截。
     */
    @Test
    void shouldRejectPostTypeUpdateForNonPostDept() {
        DeptUpdateETO eto = new DeptUpdateETO();
        eto.setId(2L);
        eto.setName("研发部");
        eto.setCode("DEV");
        eto.setPostType("BACKEND_ENGINEER");
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        when(userDeptMapper.selectById(2L)).thenReturn(buildDept(2L, 1L, "DEPT", null, "/1/2/", 2));

        BizException exception = assertThrows(BizException.class, () -> deptService.update(eto));

        assertEquals("非岗位组织不能设置岗位类型", exception.getMessage());
    }

    /**
     * 岗位节点更新时允许修改岗位类型。
     */
    @Test
    void shouldAllowUpdatingPostTypeForPostDept() {
        DeptUpdateETO eto = new DeptUpdateETO();
        eto.setId(3L);
        eto.setName("后端岗");
        eto.setCode("JAVA");
        eto.setPostType("ARCHITECT");
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        UserDept oldDept = buildDept(3L, 2L, "POST", "BACKEND_ENGINEER", "/1/2/3/", 3);
        UserDept updatedDept = buildDept(3L, 2L, "POST", "ARCHITECT", "/1/2/3/", 3);
        updatedDept.setName("后端岗");
        updatedDept.setCode("JAVA");

        when(userDeptMapper.selectById(3L)).thenReturn(oldDept, updatedDept);
        when(userDeptMapper.selectById(2L)).thenReturn(buildDept(2L, 1L, "DEPT", null, "/1/2/", 2));
        when(userDeptStructMapper.toUpdatedEntity(eq(eto), eq(oldDept))).thenReturn(updatedDept);
        when(userDeptStructMapper.toTarget(updatedDept)).thenReturn(new DeptVO());

        deptService.update(eto);

        verify(userDeptMapper).updateById(argThat(dept -> "ARCHITECT".equals(dept.getPostType())));
        verify(userDeptRelExpandService).rebuildByDeptPaths(java.util.List.of("/1/2/3/"));
        verify(userRoleDeptExpandService).rebuildByDeptPaths(java.util.List.of("/1/2/3/"));
        verify(workflowNodeApproverDeptExpandService).rebuildByDeptPaths(java.util.List.of("/1/2/3/"));
    }

    /**
     * 移动组织后需要重建旧父链与新父链下受影响用户的展开关系。
     */
    @Test
    void shouldRebuildAffectedUsersWhenMovingDept() {
        DeptMoveETO eto = new DeptMoveETO();
        eto.setId(2L);
        eto.setTargetParentId(9L);

        UserDept currentDept = buildDept(2L, 1L, "DEPT", null, "/1/2/", 2);
        UserDept targetParent = buildDept(9L, 8L, "DEPT", null, "/8/9/", 2);
        UserDept childDept = buildDept(3L, 2L, "POST", "BACKEND_ENGINEER", "/1/2/3/", 3);

        when(userDeptMapper.selectById(2L)).thenReturn(currentDept);
        when(userDeptMapper.selectById(9L)).thenReturn(targetParent);
        when(userDeptMapper.selectList(any())).thenReturn(java.util.List.of(currentDept, childDept));

        deptService.move(eto);

        verify(userDeptRelExpandService).rebuildByDeptPaths(java.util.List.of("/1/2/", "/8/9/2/"));
        verify(userRoleDeptExpandService).rebuildByDeptPaths(java.util.List.of("/1/2/", "/8/9/2/"));
        verify(workflowNodeApproverDeptExpandService).rebuildByDeptPaths(java.util.List.of("/1/2/", "/8/9/2/"));
    }

    /**
     * 查询组织下用户时应读取展开关系并按用户去重。
     */
    @Test
    void shouldQueryDistinctUsersFromExpandedRelations() {
        UserDept dept = buildDept(2L, 1L, "DEPT", null, "/1/2/", 2);
        UserDeptRelExpand relation1 = new UserDeptRelExpand();
        relation1.setDeptId(2L);
        relation1.setUserId(11L);
        UserDeptRelExpand relation2 = new UserDeptRelExpand();
        relation2.setDeptId(2L);
        relation2.setUserId(11L);
        UserDeptRelExpand relation3 = new UserDeptRelExpand();
        relation3.setDeptId(2L);
        relation3.setUserId(12L);

        User user1 = new User();
        user1.setId(11L);
        User user2 = new User();
        user2.setId(12L);
        UserSimpleVO userVO1 = new UserSimpleVO();
        userVO1.setId(11L);
        UserSimpleVO userVO2 = new UserSimpleVO();
        userVO2.setId(12L);

        when(userDeptMapper.selectById(2L)).thenReturn(dept);
        when(userDeptRelExpandMapper.selectList(any())).thenReturn(java.util.List.of(relation1, relation2, relation3));
        when(userMapper.selectBatchIds(java.util.List.of(11L, 12L))).thenReturn(java.util.List.of(user1, user2));
        when(userStructMapper.toUserSimpleVO(user1)).thenReturn(userVO1);
        when(userStructMapper.toUserSimpleVO(user2)).thenReturn(userVO2);

        java.util.List<UserSimpleVO> result = deptService.getUsers(2L);

        assertEquals(2, result.size());
    }

    /**
     * 构造组织测试对象。
     */
    private UserDept buildDept(Long id, Long parentId, String orgType, String postType, String path, Integer level) {
        UserDept dept = new UserDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setOrgType(orgType);
        dept.setPostType(postType);
        dept.setPath(path);
        dept.setLevel(level);
        dept.setStatus(CommonStatusEnum.ENABLED.getId());
        return dept;
    }
}

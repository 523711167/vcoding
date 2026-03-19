package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.UserDeptStructMapper;
import com.yuyu.workflow.convert.UserStructMapper;
import com.yuyu.workflow.eto.dept.DeptCreateETO;
import com.yuyu.workflow.eto.dept.DeptMoveETO;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private UserDeptStructMapper userDeptStructMapper;

    @Mock
    private UserStructMapper userStructMapper;

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

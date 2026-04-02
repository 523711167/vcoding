package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.DataScopeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.entity.UserDeptRelExpand;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.entity.UserRoleDeptExpand;
import com.yuyu.workflow.qto.workflow.WorkflowQueryListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoPageQTO;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.service.RoleService;
import com.yuyu.workflow.service.UserDeptRelExpandService;
import com.yuyu.workflow.service.UserDeptRelService;
import com.yuyu.workflow.service.UserRoleDeptExpandService;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
import com.yuyu.workflow.struct.WorkflowQueryStructMapper;
import com.yuyu.workflow.struct.WorkflowTodoStructMapper;
import com.yuyu.workflow.vo.workflow.WorkflowQueryVO;
import com.yuyu.workflow.vo.workflow.WorkflowTodoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作流业务查询服务测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowBizQueryServiceImplTests {

    @Mock
    private BizApplyService bizApplyService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserDeptRelService userDeptRelService;

    @Mock
    private UserDeptRelExpandService userDeptRelExpandService;

    @Mock
    private UserRoleDeptExpandService userRoleDeptExpandService;

    @Mock
    private WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService;

    @Mock
    private WorkflowQueryStructMapper workflowQueryStructMapper;

    @Mock
    private WorkflowTodoStructMapper workflowTodoStructMapper;

    private WorkflowBizQueryServiceImpl workflowBizQueryService;

    @BeforeEach
    void setUp() {
        workflowBizQueryService = new WorkflowBizQueryServiceImpl(
                bizApplyService,
                roleService,
                userDeptRelService,
                userDeptRelExpandService,
                userRoleDeptExpandService,
                workflowNodeApproverInstanceService,
                workflowQueryStructMapper,
                workflowTodoStructMapper
        );
    }

    /**
     * 代办列表应返回状态文案并透传查询条件。
     */
    @Test
    void shouldReturnTodoListWithStatusMessage() {
        WorkflowTodoListQTO qto = new WorkflowTodoListQTO();
        qto.setCurrentUserId(1L);
        qto.setBizApplyId(101L);
        qto.setTitle("报销");

        WorkflowTodoVO todo = new WorkflowTodoVO();
        todo.setApproverInstanceId(1001L);
        todo.setApproverStatus("PENDING");
        when(workflowNodeApproverInstanceService.listTodos(eq(qto)))
                .thenReturn(List.of(todo));
        WorkflowTodoVO converted = new WorkflowTodoVO();
        converted.setApproverInstanceId(1001L);
        converted.setApproverStatusMsg("待处理");
        when(workflowTodoStructMapper.toTargetList(List.of(todo))).thenReturn(List.of(converted));

        List<WorkflowTodoVO> result = workflowBizQueryService.todoList(qto);

        assertEquals(1, result.size());
        WorkflowTodoVO vo = result.get(0);
        assertEquals(1001L, vo.getApproverInstanceId());
        assertEquals("待处理", vo.getApproverStatusMsg());
        verify(workflowNodeApproverInstanceService).listTodos(eq(qto));
    }

    /**
     * 分页查询应包含总数和记录。
     */
    @Test
    void shouldReturnTodoPage() {
        WorkflowTodoPageQTO qto = new WorkflowTodoPageQTO();
        qto.setCurrentUserId(1L);
        qto.setPageNum(1L);
        qto.setPageSize(10L);
        qto.setBizApplyId(102L);
        qto.setTitle("采购");

        WorkflowTodoVO todo = new WorkflowTodoVO();
        todo.setApproverStatus("PENDING");
        Page<WorkflowTodoVO> resultPage = new Page<>(1L, 10L, 1L);
        resultPage.setRecords(List.of(todo));
        when(workflowNodeApproverInstanceService.pageTodos(any(Page.class), any(WorkflowTodoPageQTO.class)))
                .thenReturn(resultPage);
        WorkflowTodoVO converted = new WorkflowTodoVO();
        converted.setApproverStatusMsg("待处理");
        when(workflowTodoStructMapper.toTargetList(List.of(todo))).thenReturn(List.of(converted));

        PageVo<WorkflowTodoVO> page = workflowBizQueryService.todoPage(qto);

        assertEquals(1L, page.total());
        assertEquals(1, page.records().size());
        assertEquals("待处理", page.records().get(0).getApproverStatusMsg());
    }

    /**
     * 无总数时应直接返回空分页。
     */
    @Test
    void shouldReturnEmptyPageWhenTotalIsZero() {
        WorkflowTodoPageQTO qto = new WorkflowTodoPageQTO();
        qto.setCurrentUserId(1L);
        qto.setPageNum(2L);
        qto.setPageSize(5L);

        Page<WorkflowTodoVO> resultPage = new Page<>(2L, 5L, 0L);
        resultPage.setRecords(Collections.emptyList());
        when(workflowNodeApproverInstanceService.pageTodos(any(Page.class), any(WorkflowTodoPageQTO.class)))
                .thenReturn(resultPage);
        when(workflowTodoStructMapper.toTargetList(Collections.emptyList())).thenReturn(Collections.emptyList());

        PageVo<WorkflowTodoVO> page = workflowBizQueryService.todoPage(qto);
        assertEquals(0L, page.total());
        assertTrue(page.records().isEmpty());
    }

    /**
     * 当前用户为空时应抛异常。
     */
    @Test
    void shouldThrowWhenCurrentUserIsNull() {
        WorkflowTodoListQTO qto = new WorkflowTodoListQTO();
        BizException ex = assertThrows(BizException.class, () -> workflowBizQueryService.todoList(qto));
        assertEquals("当前用户不能为空", ex.getMessage());
    }

    /**
     * 查询箱存在 ALL 数据权限时应跳过组织过滤。
     */
    @Test
    void shouldMarkHasAllDataWhenRoleScopeContainsAll() {
        WorkflowQueryListQTO qto = new WorkflowQueryListQTO();
        qto.setCurrentUserId(1L);

        when(roleService.listEnabledRoleIdsByUserId(1L)).thenReturn(List.of(11L, 12L));
        when(roleService.listByIds(List.of(11L, 12L))).thenReturn(List.of(
                buildRole(11L, DataScopeEnum.ALL.getCode()),
                buildRole(12L, DataScopeEnum.CURRENT_DEPT.getCode())
        ));
        when(bizApplyService.listQueries(eq(qto))).thenReturn(Collections.emptyList());
        when(workflowQueryStructMapper.toTargetList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<WorkflowQueryVO> result = workflowBizQueryService.queryList(qto);

        assertTrue(result.isEmpty());
        assertTrue(Boolean.TRUE.equals(qto.getHasAllData()));
        assertTrue(qto.getVisibleDeptIdList().isEmpty());
        verify(userDeptRelService, never()).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(userDeptRelExpandService, never()).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 当前组织与当前组织及子组织同时存在时，应取当前组织及子组织。
     */
    @Test
    void shouldUseCurrentAndChildDeptWhenBothCurrentScopesExist() {
        WorkflowQueryListQTO qto = new WorkflowQueryListQTO();
        qto.setCurrentUserId(2L);

        when(roleService.listEnabledRoleIdsByUserId(2L)).thenReturn(List.of(21L, 22L));
        when(roleService.listByIds(List.of(21L, 22L))).thenReturn(List.of(
                buildRole(21L, DataScopeEnum.CURRENT_DEPT.getCode()),
                buildRole(22L, DataScopeEnum.CURRENT_AND_CHILD_DEPT.getCode())
        ));
        when(userDeptRelExpandService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                buildUserDeptRelExpand(200L),
                buildUserDeptRelExpand(201L)
        ));
        when(bizApplyService.listQueries(eq(qto))).thenReturn(Collections.emptyList());
        when(workflowQueryStructMapper.toTargetList(Collections.emptyList())).thenReturn(Collections.emptyList());

        workflowBizQueryService.queryList(qto);

        assertFalse(Boolean.TRUE.equals(qto.getHasAllData()));
        assertEquals(Set.of(200L, 201L), Set.copyOf(qto.getVisibleDeptIdList()));
    }

    /**
     * 自选组织与当前组织同时存在时，应取交集。
     */
    @Test
    void shouldUseIntersectionWhenCustomAndCurrentDeptBothExist() {
        WorkflowQueryListQTO qto = new WorkflowQueryListQTO();
        qto.setCurrentUserId(3L);

        when(roleService.listEnabledRoleIdsByUserId(3L)).thenReturn(List.of(31L, 32L));
        when(roleService.listByIds(List.of(31L, 32L))).thenReturn(List.of(
                buildRole(31L, DataScopeEnum.CURRENT_DEPT.getCode()),
                buildRole(32L, DataScopeEnum.CUSTOM_DEPT.getCode())
        ));
        when(userDeptRelService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                buildUserDeptRel(300L),
                buildUserDeptRel(301L)
        ));
        when(userRoleDeptExpandService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                buildUserRoleDeptExpand(301L),
                buildUserRoleDeptExpand(302L)
        ));
        when(bizApplyService.listQueries(eq(qto))).thenReturn(Collections.emptyList());
        when(workflowQueryStructMapper.toTargetList(Collections.emptyList())).thenReturn(Collections.emptyList());

        workflowBizQueryService.queryList(qto);

        assertFalse(Boolean.TRUE.equals(qto.getHasAllData()));
        assertEquals(Set.of(301L), Set.copyOf(qto.getVisibleDeptIdList()));
    }

    /**
     * 自选组织与当前组织及子组织同时存在时，也应取交集。
     */
    @Test
    void shouldUseIntersectionWhenCustomAndCurrentAndChildDeptBothExist() {
        WorkflowQueryListQTO qto = new WorkflowQueryListQTO();
        qto.setCurrentUserId(6L);

        when(roleService.listEnabledRoleIdsByUserId(6L)).thenReturn(List.of(61L, 62L));
        when(roleService.listByIds(List.of(61L, 62L))).thenReturn(List.of(
                buildRole(61L, DataScopeEnum.CURRENT_AND_CHILD_DEPT.getCode()),
                buildRole(62L, DataScopeEnum.CUSTOM_DEPT.getCode())
        ));
        when(userDeptRelExpandService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                buildUserDeptRelExpand(600L),
                buildUserDeptRelExpand(601L)
        ));
        when(userRoleDeptExpandService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                buildUserRoleDeptExpand(601L),
                buildUserRoleDeptExpand(602L)
        ));
        when(bizApplyService.listQueries(eq(qto))).thenReturn(Collections.emptyList());
        when(workflowQueryStructMapper.toTargetList(Collections.emptyList())).thenReturn(Collections.emptyList());

        workflowBizQueryService.queryList(qto);

        assertFalse(Boolean.TRUE.equals(qto.getHasAllData()));
        assertEquals(Set.of(601L), Set.copyOf(qto.getVisibleDeptIdList()));
    }

    /**
     * 无角色时也应走查询，由 SQL 保证“本人可见”。
     */
    @Test
    void shouldStillQueryWhenNoRoleScope() {
        WorkflowQueryListQTO qto = new WorkflowQueryListQTO();
        qto.setCurrentUserId(4L);

        when(roleService.listEnabledRoleIdsByUserId(4L)).thenReturn(Collections.emptyList());
        when(bizApplyService.listQueries(eq(qto))).thenReturn(Collections.emptyList());
        when(workflowQueryStructMapper.toTargetList(Collections.emptyList())).thenReturn(Collections.emptyList());

        workflowBizQueryService.queryList(qto);

        assertFalse(Boolean.TRUE.equals(qto.getHasAllData()));
        assertTrue(qto.getVisibleDeptIdList().isEmpty());
        verify(bizApplyService).listQueries(eq(qto));
    }

    /**
     * 仅本人数据权限时，不应放开组织范围。
     */
    @Test
    void shouldLimitToSelfWhenScopeIsSelf() {
        WorkflowQueryListQTO qto = new WorkflowQueryListQTO();
        qto.setCurrentUserId(5L);

        when(roleService.listEnabledRoleIdsByUserId(5L)).thenReturn(List.of(51L));
        when(roleService.listByIds(List.of(51L))).thenReturn(List.of(
                buildRole(51L, DataScopeEnum.SELF.getCode())
        ));
        when(bizApplyService.listQueries(eq(qto))).thenReturn(Collections.emptyList());
        when(workflowQueryStructMapper.toTargetList(Collections.emptyList())).thenReturn(Collections.emptyList());

        workflowBizQueryService.queryList(qto);

        assertFalse(Boolean.TRUE.equals(qto.getHasAllData()));
        assertTrue(qto.getVisibleDeptIdList().isEmpty());
        verify(userDeptRelService, never()).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(userDeptRelExpandService, never()).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(userRoleDeptExpandService, never()).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    private UserRole buildRole(Long id, String dataScope) {
        UserRole role = new UserRole();
        role.setId(id);
        role.setDataScope(dataScope);
        return role;
    }

    private UserDeptRel buildUserDeptRel(Long deptId) {
        UserDeptRel rel = new UserDeptRel();
        rel.setDeptId(deptId);
        return rel;
    }

    private UserDeptRelExpand buildUserDeptRelExpand(Long deptId) {
        UserDeptRelExpand relExpand = new UserDeptRelExpand();
        relExpand.setDeptId(deptId);
        return relExpand;
    }

    private UserRoleDeptExpand buildUserRoleDeptExpand(Long deptId) {
        UserRoleDeptExpand roleDeptExpand = new UserRoleDeptExpand();
        roleDeptExpand.setDeptId(deptId);
        return roleDeptExpand;
    }
}

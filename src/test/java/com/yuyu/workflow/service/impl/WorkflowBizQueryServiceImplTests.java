package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.mapper.BizApplyMapper;
import com.yuyu.workflow.mapper.UserDeptRelExpandMapper;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.mapper.UserRoleDeptExpandMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverInstanceMapper;
import com.yuyu.workflow.struct.WorkflowQueryStructMapper;
import com.yuyu.workflow.qto.workflow.WorkflowTodoListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoPageQTO;
import com.yuyu.workflow.struct.WorkflowTodoStructMapper;
import com.yuyu.workflow.vo.workflow.WorkflowTodoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作流业务查询服务测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowBizQueryServiceImplTests {

    @Mock
    private BizApplyMapper bizApplyMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserDeptRelMapper userDeptRelMapper;

    @Mock
    private UserDeptRelExpandMapper userDeptRelExpandMapper;

    @Mock
    private UserRoleDeptExpandMapper userRoleDeptExpandMapper;

    @Mock
    private WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper;

    @Mock
    private WorkflowQueryStructMapper workflowQueryStructMapper;

    @Mock
    private WorkflowTodoStructMapper workflowTodoStructMapper;

    private WorkflowBizQueryServiceImpl workflowBizQueryService;

    @BeforeEach
    void setUp() {
        workflowBizQueryService = new WorkflowBizQueryServiceImpl(
                bizApplyMapper,
                userRoleMapper,
                userDeptRelMapper,
                userDeptRelExpandMapper,
                userRoleDeptExpandMapper,
                workflowNodeApproverInstanceMapper,
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
        when(workflowNodeApproverInstanceMapper.selectTodoList(eq(qto)))
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
        verify(workflowNodeApproverInstanceMapper).selectTodoList(eq(qto));
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
        when(workflowNodeApproverInstanceMapper.selectTodoPage(any(Page.class), any(WorkflowTodoPageQTO.class)))
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
        when(workflowNodeApproverInstanceMapper.selectTodoPage(any(Page.class), any(WorkflowTodoPageQTO.class)))
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
}

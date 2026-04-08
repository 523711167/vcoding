package com.yuyu.workflow.service;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.qto.workflow.WorkflowQueryDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryPageQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoPageQTO;
import com.yuyu.workflow.vo.workflow.WorkflowQueryVO;
import com.yuyu.workflow.vo.workflow.WorkflowTodoVO;

import java.util.List;

/**
 * 工作流业务查询服务。
 */
public interface WorkflowBizQueryService {

    /**
     * 查询当前用户代办箱列表。
     */
    List<WorkflowTodoVO> todoList(WorkflowTodoListQTO qto);

    /**
     * 分页查询当前用户代办箱列表。
     */
    PageVo<WorkflowTodoVO> todoPage(WorkflowTodoPageQTO qto);

    /**
     * 查询当前用户代办箱详情。
     */
    WorkflowTodoVO todoDetail(WorkflowTodoDetailQTO qto);

    /**
     * 查询当前用户已办箱列表。
     */
    List<WorkflowTodoVO> processedList(WorkflowTodoListQTO qto);

    /**
     * 分页查询当前用户已办箱列表。
     */
    PageVo<WorkflowTodoVO> processedPage(WorkflowTodoPageQTO qto);

    /**
     * 查询当前用户已办箱详情。
     */
    WorkflowTodoVO processedDetail(WorkflowTodoDetailQTO qto);

    /**
     * 查询当前用户查询箱列表。
     */
    List<WorkflowQueryVO> queryList(WorkflowQueryListQTO qto);

    /**
     * 分页查询当前用户查询箱列表。
     */
    PageVo<WorkflowQueryVO> queryPage(WorkflowQueryPageQTO qto);

    /**
     * 查询当前用户查询箱详情。
     */
    WorkflowQueryVO queryDetail(WorkflowQueryDetailQTO qto);
}

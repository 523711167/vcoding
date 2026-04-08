package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.common.enums.WorkflowNodeApproverInstanceStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeInstanceStatusEnum;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowRejectAuditETO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoPageQTO;
import com.yuyu.workflow.vo.workflow.WorkflowTodoVO;

import java.util.List;

/**
 * 节点审批人实例服务接口。
 */
public interface WorkflowNodeApproverInstanceService extends IService<WorkflowNodeApproverInstance> {

    /**
     * 按流程实例主键集合查询节点审批人实例。
     */
    List<WorkflowNodeApproverInstance> listByInstanceIds(List<Long> instanceIdList);

    /**
     * 按节点实例主键集合查询节点审批人实例。
     */
    List<WorkflowNodeApproverInstance> listByNodeInstanceIds(List<Long> nodeInstanceIdList);

    /**
     * 按流程实例主键集合删除节点审批人实例。
     */
    void removeByInstanceIds(List<Long> instanceIdList);

    /**
     * 按节点实例主键集合删除节点审批人实例。
     */
    void removeByNodeInstanceIds(List<Long> nodeInstanceIdList);

    /**
     * 审批驳回。
     *
     * @param workflowRejectAuditETO
     */
    void reject(WorkflowRejectAuditETO workflowRejectAuditETO);

    /**
     * 审核拒绝，修改状态
     */
    void updateNodeApproverForReject(Long nodeInstanceId, String comment, Long approverInstanceId);

    void updateNodeApproverForApprove(Long nodeInstanceId, String comment, Long approverInstanceId);

    void updateNodeApprover(Long nodeInstanceId, String comment, Long approverUserId, WorkflowNodeApproverInstanceStatusEnum approverEnum);

    WorkflowNodeInstanceStatusEnum activateNextApproverInstance(WorkflowNodeApproverInstance current, List<WorkflowNodeApproverInstance> approverInstanceList);

    void cancelOtherPendingApprovers(Long instanceId, Long nodeInstanceId, Long approverInstanceId);

    /**
     * 取消流程时批量关闭待处理审批人实例。
     */
    void cancelPendingApproversForInstance(Long instanceId);

    void saveApproverInstancesForUser(WorkflowNodeInstance workflowNodeInstance);

    void saveApproverInstancesForRole(WorkflowNodeInstance workflowNodeInstance);

    void saveApproverInstancesFordept(WorkflowNodeInstance workflowNodeInstance);

    /**
     * 查询代办箱列表。
     */
    List<WorkflowTodoVO> listTodos(WorkflowTodoListQTO qto);

    /**
     * 查询代办箱分页。
     */
    IPage<WorkflowTodoVO> pageTodos(IPage<WorkflowTodoVO> page, WorkflowTodoPageQTO qto);

    /**
     * 查询代办箱详情。
     */
    WorkflowTodoVO detailTodo(WorkflowTodoDetailQTO qto);

    /**
     * 查询已办箱列表。
     */
    List<WorkflowTodoVO> listProcessed(WorkflowTodoListQTO qto);

    /**
     * 查询已办箱分页。
     */
    IPage<WorkflowTodoVO> pageProcessed(IPage<WorkflowTodoVO> page, WorkflowTodoPageQTO qto);

    /**
     * 查询已办箱详情。
     */
    WorkflowTodoVO detailProcessed(WorkflowTodoDetailQTO qto);

}

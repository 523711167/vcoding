package com.yuyu.workflow.service;

import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.eto.workflow.WorkflowBizSubmitETO;
import com.yuyu.workflow.eto.workflow.WorkflowCancelETO;
import com.yuyu.workflow.eto.workflow.WorkflowDelegateETO;

/**
 * 工作流发起服务。
 */
public interface WorkflowLaunchService {

    /**
     * 审核流程
     *   节点加签拒绝 (发起加签人无需修改)
     *   串行节点拒绝   status = REJECTED  finished_at  comment
     *   status = REJECTED  finished_at
     *   status = REJECTED  finished_at
     *   action = REJECT     to_node_id = null
     *
     *   并行节点拒绝 修改 当前节点审核人实例      status = REJECTED  finished_at
     *   修改 当前节点实例           所有分支审核完毕后修改
     *   修改 当前流程实例           所有分支审核完毕后修改
     *   插入 审核记录               action = REJECT     to_node_id = 聚合节点
     * @param eto
     */
    void audit(WorkflowAuditETO eto);

    void submit(WorkflowBizSubmitETO eto);

    /**
     * 发起人取消运行中的流程实例。
     */
    void cancel(WorkflowCancelETO eto);

    /**
     * 当前审批人将待办转交给其他用户。
     */
    void delegate(WorkflowDelegateETO eto);
}

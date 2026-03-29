package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.common.enums.WorkflowNodeApproverInstanceStatusEnum;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowRejectAuditETO;

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

    boolean activateNextApproverInstance(WorkflowNodeApproverInstance current, List<WorkflowNodeApproverInstance> approverInstanceList);

    void cancelOtherPendingApprovers(Long instanceId, Long nodeInstanceId, Long approverInstanceId);

    void saveApproverInstancesForUser(WorkflowNodeInstance workflowNodeInstance);

    void saveApproverInstancesForRole(WorkflowNodeInstance workflowNodeInstance);

    void saveApproverInstancesFordept(WorkflowNodeInstance workflowNodeInstance);


}

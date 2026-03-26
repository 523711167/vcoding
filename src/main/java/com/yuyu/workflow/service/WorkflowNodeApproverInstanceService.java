package com.yuyu.workflow.service;

import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.eto.workflow.WorkflowRejectAuditETO;

import java.util.List;

/**
 * 节点审批人实例服务接口。
 */
public interface WorkflowNodeApproverInstanceService {

    /**
     * 新增节点审批人实例。
     */
    void save(WorkflowNodeApproverInstance workflowNodeApproverInstance);

    /**
     * 批量新增节点审批人实例。
     */
    void saveBatch(List<WorkflowNodeApproverInstance> workflowNodeApproverInstanceList);

    /**
     * 按流程实例主键集合查询节点审批人实例。
     */
    List<WorkflowNodeApproverInstance> listByInstanceIds(List<Long> instanceIdList);

    /**
     * 按节点实例主键集合查询节点审批人实例。
     */
    List<WorkflowNodeApproverInstance> listByNodeInstanceIds(List<Long> nodeInstanceIdList);

    /**
     * 按主键更新节点审批人实例。
     */
    void updateById(WorkflowNodeApproverInstance workflowNodeApproverInstance);

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
    void updateNodeApproverForReject(Long nodeInstanceId, String comment);

}

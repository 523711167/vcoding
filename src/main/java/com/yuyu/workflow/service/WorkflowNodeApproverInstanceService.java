package com.yuyu.workflow.service;

import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;

import java.util.List;

/**
 * 节点审批人实例服务接口。
 */
public interface WorkflowNodeApproverInstanceService {

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
}

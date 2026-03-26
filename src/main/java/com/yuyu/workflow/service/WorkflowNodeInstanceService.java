package com.yuyu.workflow.service;

import com.yuyu.workflow.entity.WorkflowNodeInstance;

import java.util.List;

/**
 * 节点实例服务接口。
 */
public interface WorkflowNodeInstanceService {

    /**
     * 新增节点实例。
     */
    void save(WorkflowNodeInstance workflowNodeInstance);

    /**
     * 批量新增节点实例。
     */
    void saveBatch(List<WorkflowNodeInstance> workflowNodeInstanceList);

    /**
     * 按流程实例主键集合查询节点实例。
     */
    List<WorkflowNodeInstance> listByInstanceIds(List<Long> instanceIdList);

    /**
     * 按主键更新节点实例。
     */
    void updateById(WorkflowNodeInstance workflowNodeInstance);

    /**
     * 按流程实例主键集合删除节点实例。
     */
    void removeByInstanceIds(List<Long> instanceIdList);

    /**
     * 审核拒绝，节点修改状态
     */
    void updateNodeForReject(Long nodeInstanceId, String comment);
}

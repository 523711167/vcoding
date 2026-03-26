package com.yuyu.workflow.service;

import com.yuyu.workflow.entity.WorkflowInstance;

import java.util.List;

/**
 * 流程实例服务接口。
 */
public interface WorkflowInstanceService {

    /**
     * 新增流程实例。
     */
    void save(WorkflowInstance workflowInstance);

    /**
     * 按主键查询流程实例，不存在时抛出异常。
     */
    WorkflowInstance getByIdOrThrow(Long id);

    /**
     * 按主键更新流程实例。
     */
    void updateById(WorkflowInstance workflowInstance);

    /**
     * 按主键集合删除流程实例。
     */
    void removeByIds(List<Long> idList);

    /**
     * 审核拒绝修改流程实例状态
     */
    void updateNodeForReject(Long instanceId);
}

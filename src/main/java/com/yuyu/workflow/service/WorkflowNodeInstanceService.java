package com.yuyu.workflow.service;

import com.yuyu.workflow.entity.WorkflowNodeInstance;

import java.util.List;

/**
 * 节点实例服务接口。
 */
public interface WorkflowNodeInstanceService {

    /**
     * 按流程实例主键集合查询节点实例。
     */
    List<WorkflowNodeInstance> listByInstanceIds(List<Long> instanceIdList);

    /**
     * 按流程实例主键集合删除节点实例。
     */
    void removeByInstanceIds(List<Long> instanceIdList);
}

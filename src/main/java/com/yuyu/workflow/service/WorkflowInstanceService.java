package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;

/**
 * 流程实例服务接口。
 */
public interface WorkflowInstanceService extends IService<WorkflowInstance> {

    /**
     * 按主键查询流程实例，不存在时抛出异常。
     */
    WorkflowInstance getByIdOrThrow(Long id);

    /**
     * 审核拒绝修改流程实例状态
     */
    void updateNodeForReject(Long instanceId, WorkflowNodeInstance workflowNodeInstance);
}

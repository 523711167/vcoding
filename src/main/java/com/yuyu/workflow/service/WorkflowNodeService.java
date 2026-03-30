package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.WorkflowNode;

/**
 * 流程节点服务接口。
 */
public interface WorkflowNodeService extends IService<WorkflowNode> {

    /**
     * 按主键查询流程节点，不存在时抛出异常。
     */
    WorkflowNode getByIdOrThrow(Long id);
}

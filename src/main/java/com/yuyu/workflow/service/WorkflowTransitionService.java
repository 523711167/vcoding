package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.WorkflowTransition;

import java.util.List;

/**
 * 流程连线服务接口。
 */
public interface WorkflowTransitionService extends IService<WorkflowTransition> {

    /**
     * 按主键查询流程连线，不存在时抛出异常。
     */
    WorkflowTransition getByIdOrThrow(Long id);

    /**
     * 按流程定义ID查询连线列表。
     */
    List<WorkflowTransition> listByDefinitionId(Long definitionId);

    /**
     * 按来源节点ID查询连线列表。
     */
    List<WorkflowTransition> listByFromNodeId(Long fromNodeId);

    /**
     * 按流程定义ID集合删除连线。
     */
    void removeByDefinitionIds(List<Long> definitionIds);
}

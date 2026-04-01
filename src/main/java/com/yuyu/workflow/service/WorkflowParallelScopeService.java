package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowParallelScope;

import java.util.List;

/**
 * 流程并行作用域服务接口。
 */
public interface WorkflowParallelScopeService extends IService<WorkflowParallelScope> {

    /**
     * 按流程实例主键集合查询并行作用域。
     */
    List<WorkflowParallelScope> listByInstanceIds(List<Long> instanceIdList);

    /**
     * 按流程实例主键集合删除并行作用域。
     */
    void removeByInstanceIds(List<Long> instanceIdList);

    /**
     * 首次进入并行拆分节点时创建并行作用域。
     */
    WorkflowParallelScope createOnParallelSplitEnter(WorkflowInstance workflowInstance,
                                                     WorkflowNode splitDefinitionNode,
                                                     WorkflowNode joinDefinitionNode,
                                                     Long parentScopeId,
                                                     Integer expectedBranchCount);

    void markParallelBranchArrived(Long parallelScopeId);

    Long getParentScopeId(Long parallelScopeId);
}

package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
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
}

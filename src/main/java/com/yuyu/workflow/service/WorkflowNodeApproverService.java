package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.WorkflowNodeApprover;

import java.util.List;

/**
 * 节点审批人配置服务接口。
 */
public interface WorkflowNodeApproverService extends IService<WorkflowNodeApprover> {

    /**
     * 按节点主键集合查询审批人配置。
     */
    List<WorkflowNodeApprover> listByNodeIds(List<Long> nodeIds);

    /**
     * 按节点主键集合删除审批人配置。
     */
    void removeByNodeIds(List<Long> nodeIds);

    /**
     * 批量保存审批人配置。
     */
    void saveApprovers(List<WorkflowNodeApprover> approverList);
}

package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.common.enums.WorkflowNodeInstanceStatusEnum;
import com.yuyu.workflow.entity.WorkflowNodeInstance;

import java.util.List;

/**
 * 节点实例服务接口。
 */
public interface WorkflowNodeInstanceService extends IService<WorkflowNodeInstance> {

    /**
     * 按流程实例主键集合查询节点实例。
     */
    List<WorkflowNodeInstance> listByInstanceIds(List<Long> instanceIdList);

    /**
     * 按流程实例主键集合删除节点实例。
     */
    void removeByInstanceIds(List<Long> instanceIdList);

    /**
     * 审核拒绝，节点修改状态
     */
    void updateNodeForReject(Long nodeInstanceId, String comment);

    void updateNodeForApprove(Long nodeInstanceId, String comment);

    void updateNode(Long nodeInstanceId, String comment, WorkflowNodeInstanceStatusEnum nodeInstanceEnum);
}

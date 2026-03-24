package com.yuyu.workflow.service;

import com.yuyu.workflow.entity.WorkflowApprovalRecord;

import java.util.List;

/**
 * 审批操作记录服务接口。
 */
public interface WorkflowApprovalRecordService {

    /**
     * 按流程实例主键集合查询审批操作记录。
     */
    List<WorkflowApprovalRecord> listByInstanceIds(List<Long> instanceIdList);

    /**
     * 按流程实例主键集合删除审批操作记录。
     */
    void removeByInstanceIds(List<Long> instanceIdList);
}

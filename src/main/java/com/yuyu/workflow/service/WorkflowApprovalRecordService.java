package com.yuyu.workflow.service;

import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.eto.workflow.WorkflowRejectAuditETO;

import java.util.List;

/**
 * 审批操作记录服务接口。
 */
public interface WorkflowApprovalRecordService {

    /**
     * 新增审批操作记录。
     */
    void save(WorkflowApprovalRecord workflowApprovalRecord);

    /**
     * 批量新增审批操作记录。
     */
    void saveBatch(List<WorkflowApprovalRecord> workflowApprovalRecordList);

    /**
     * 按流程实例主键集合查询审批操作记录。
     */
    List<WorkflowApprovalRecord> listByInstanceIds(List<Long> instanceIdList);

    /**
     * 按流程实例主键集合删除审批操作记录。
     */
    void removeByInstanceIds(List<Long> instanceIdList);

    /**
     * 审核人拒绝，写入审批记录
     */
    void insertRecordForReject(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance);

    /**
     * 判断当前驳回节点的上一跳是否来自并行拆分节点
     */
    boolean isPreviousNodeParallelSplit(Long instanceId, Long nodeInstanceId);
}

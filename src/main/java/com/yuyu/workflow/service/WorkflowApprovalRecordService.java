package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.eto.workflow.WorkflowCancelETO;
import com.yuyu.workflow.eto.workflow.WorkflowDelegateETO;

import java.util.List;

/**
 * 审批操作记录服务接口。
 */
public interface WorkflowApprovalRecordService extends IService<WorkflowApprovalRecord> {

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
    void recordForReject(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance);

    /**
     * 审核人拒绝，写入审批记录
     */
    void recordForApprove(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance);

    /**
     * 审核人拒绝，写入审批记录
     */
    void recordForRoute(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance, WorkflowNodeInstance toWorkflowNodeInstance);

    void recordForSplitTrigger(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance, WorkflowNodeInstance toWorkflowNodeInstance);

    void recordForJoinArrive(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance, WorkflowNodeInstance toWorkflowNodeInstance);

    void recordForJoinPass(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance, WorkflowNodeInstance toWorkflowNodeInstance);

    /**
     * 发起人取消流程，写入审批记录。
     */
    void recordForCancel(WorkflowCancelETO eto, WorkflowNodeInstance workflowNodeInstance);

    /**
     * 审批人转交任务，写入审批记录。
     */
    void recordForDelegate(WorkflowDelegateETO eto, WorkflowNodeInstance workflowNodeInstance, User delegateUser);

}

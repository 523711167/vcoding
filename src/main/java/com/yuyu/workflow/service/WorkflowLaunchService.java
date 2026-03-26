package com.yuyu.workflow.service;

import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.service.model.workflow.WorkflowStartCommand;
import com.yuyu.workflow.service.model.workflow.WorkflowStartResult;

/**
 * 工作流发起服务。
 */
public interface WorkflowLaunchService {

    /**
     * 开启流程。
     */
    WorkflowStartResult startWorkflow(WorkflowStartCommand command);

    /**
     * 审核流程
     * @param eto
     */
    void audit(WorkflowAuditETO eto);
}

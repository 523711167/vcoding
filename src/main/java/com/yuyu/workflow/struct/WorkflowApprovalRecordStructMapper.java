package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.eto.workflow.WorkflowCancelETO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface WorkflowApprovalRecordStructMapper {


    @Mapping(source = "nodeInstanceId", target = "workflowNodeInstance.id")
    WorkflowAuditETO toWorkflowAuditETO(WorkflowCancelETO eto, WorkflowNodeInstance workflowNodeInstance);
}

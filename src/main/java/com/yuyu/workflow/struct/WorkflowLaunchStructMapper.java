package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.eto.workflow.WorkflowBizSubmitETO;
import com.yuyu.workflow.service.impl.WorkflowLaunchServiceImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 流程定义对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface WorkflowLaunchStructMapper  {


    @Mapping(target = "instanceId", source = "startNodeInstance.instanceId")
    @Mapping(target = "nodeInstanceId", source = "startNodeInstance.id")
    @Mapping(target = "approverInstanceId", ignore = true)
    @Mapping(target = "action", constant = "APPROVE")
    @Mapping(target = "comment", ignore = true)
    WorkflowAuditETO toWorkflowAuditETO(WorkflowBizSubmitETO eto, WorkflowNodeInstance startNodeInstance);

    @Mapping(target = "eto", source = "workflowAuditETO")
    @Mapping(target = "workflowInstance", ignore = true)
    @Mapping(target = "currentNodeInstance", ignore = true)
    @Mapping(target = "currentApproverInstance", ignore = true)
    @Mapping(target = "approverInstanceList", ignore = true)
    @Mapping(target = "definitionNode", ignore = true)
    @Mapping(target = "nodeMap", ignore = true)
    @Mapping(target = "transitionsByFromNodeId", ignore = true)
    WorkflowLaunchServiceImpl.AuditContext toAuditContext(WorkflowAuditETO workflowAuditETO);
}

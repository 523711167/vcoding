package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.eto.workflow.WorkflowBizSubmitETO;
import com.yuyu.workflow.service.impl.WorkflowLaunchServiceImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

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
    @Mapping(target = "workflowInstance", source = "workflowInstance")
    @Mapping(target = "currentNodeInstance", source = "currentNodeInstance")
    @Mapping(target = "currentApproverInstance", ignore = true)
    @Mapping(target = "approverInstanceList", ignore = true)
    @Mapping(target = "definitionNode", source = "definitionNode")
    @Mapping(target = "nodeMap", source = "nodeMap")
    @Mapping(target = "transitionsByFromNodeId", source = "transitionsByFromNodeId")
    WorkflowLaunchServiceImpl.AuditContext toAuditContext(WorkflowAuditETO workflowAuditETO,
                                                          WorkflowInstance workflowInstance,
                                                          WorkflowNodeInstance currentNodeInstance,
                                                          WorkflowNode definitionNode,
                                                          Map<Long, WorkflowNode> nodeMap,
                                                          Map<Long, List<WorkflowTransition>> transitionsByFromNodeId);
}

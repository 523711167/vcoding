package com.yuyu.workflow.struct;

import com.yuyu.workflow.common.mapstruct.BaseMapper;
import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionCreateETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionNodeETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionUpdateETO;
import com.yuyu.workflow.eto.workflow.WorkflowNodeApproverETO;
import com.yuyu.workflow.eto.workflow.WorkflowTransitionETO;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.vo.workflow.WorkflowDefinitionVO;
import com.yuyu.workflow.vo.workflow.WorkflowNodeApproverVO;
import com.yuyu.workflow.vo.workflow.WorkflowNodeVO;
import com.yuyu.workflow.vo.workflow.WorkflowTransitionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 流程定义对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface WorkflowDefinitionStructMapper extends BaseMapper<WorkflowDefinition, WorkflowDefinitionVO> {

    @Override
    @Mapping(target = "workFlowJson", source = "workflowJson")
    @Mapping(target = "statusMsg", ignore = true)
    @Mapping(target = "nodeList", ignore = true)
    @Mapping(target = "transitionList", ignore = true)
    WorkflowDefinitionVO toTarget(WorkflowDefinition source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "workflowJson", source = "workFlowJson")
    WorkflowDefinition toEntity(WorkflowDefinitionCreateETO eto);

    @Mapping(target = "id", source = "oldEntity.id")
    @Mapping(target = "code", source = "oldEntity.code")
    @Mapping(target = "version", source = "oldEntity.version")
    @Mapping(target = "status", source = "oldEntity.status")
    @Mapping(target = "createdBy", source = "oldEntity.createdBy")
    @Mapping(target = "createdAt", source = "oldEntity.createdAt")
    @Mapping(target = "updatedAt", source = "oldEntity.updatedAt")
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "name", source = "eto.name")
    @Mapping(target = "description", source = "eto.description")
    @Mapping(target = "workflowJson", source = "eto.workFlowJson")
    WorkflowDefinition toUpdatedEntity(WorkflowDefinitionUpdateETO eto, WorkflowDefinition oldEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "definitionId", ignore = true)
    @Mapping(target = "parallelSplitNodeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    WorkflowNode toNodeEntity(WorkflowDefinitionNodeETO eto);

    @Mapping(target = "nodeTypeMsg", ignore = true)
    @Mapping(target = "approveModeMsg", ignore = true)
    @Mapping(target = "timeoutActionMsg", ignore = true)
    @Mapping(target = "approverList", ignore = true)
    WorkflowNodeVO toNodeVO(WorkflowNode entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "definitionId", ignore = true)
    @Mapping(target = "nodeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    WorkflowNodeApprover toApproverEntity(WorkflowNodeApproverETO eto);

    @Mapping(target = "approverTypeMsg", ignore = true)
    WorkflowNodeApproverVO toApproverVO(WorkflowNodeApprover entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "definitionId", ignore = true)
    @Mapping(target = "fromNodeId", ignore = true)
    @Mapping(target = "fromNodeName", ignore = true)
    @Mapping(target = "fromNodeType", ignore = true)
    @Mapping(target = "toNodeId", ignore = true)
    @Mapping(target = "toNodeName", ignore = true)
    @Mapping(target = "toNodeType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    WorkflowTransition toTransitionEntity(WorkflowTransitionETO eto);

    WorkflowTransitionVO toTransitionVO(WorkflowTransition entity);
}

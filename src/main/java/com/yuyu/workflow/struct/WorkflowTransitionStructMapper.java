package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.eto.workflow.WorkflowTransitionETO;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.vo.workflow.WorkflowTransitionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 流程连线对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface WorkflowTransitionStructMapper {

    /**
     * 流程连线参数转换为实体。
     */
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
    WorkflowTransition toEntity(WorkflowTransitionETO source);

    /**
     * 流程连线实体转换为视图对象。
     */
    WorkflowTransitionVO toTarget(WorkflowTransition source);
}

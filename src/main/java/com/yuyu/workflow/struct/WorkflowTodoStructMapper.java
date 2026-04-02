package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.vo.workflow.WorkflowTodoVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 代办箱对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface WorkflowTodoStructMapper {

    /**
     * 转换代办对象并补充状态文案。
     */
    @Mapping(
            target = "approverStatusMsg",
            expression = "java(com.yuyu.workflow.common.enums.WorkflowNodeApproverInstanceStatusEnum.getMsgByCode(source.getApproverStatus()))"
    )
    WorkflowTodoVO toTarget(WorkflowTodoVO source);

    /**
     * 批量转换代办对象并补充状态文案。
     */
    List<WorkflowTodoVO> toTargetList(List<WorkflowTodoVO> sourceList);
}

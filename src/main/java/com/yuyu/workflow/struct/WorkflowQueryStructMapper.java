package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.vo.workflow.WorkflowQueryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 查询箱对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface WorkflowQueryStructMapper {

    /**
     * 转换查询箱对象并补充状态文案。
     */
    @Mapping(
            target = "bizStatusMsg",
            expression = "java(com.yuyu.workflow.common.enums.BizApplyStatusEnum.getMsgByCode(source.getBizStatus()))"
    )
    @Mapping(
            target = "workflowStatusMsg",
            expression = "java(com.yuyu.workflow.common.enums.WorkflowInstanceStatusEnum.getMsgByCode(source.getWorkflowStatus()))"
    )
    WorkflowQueryVO toTarget(WorkflowQueryVO source);

    /**
     * 批量转换查询箱对象并补充状态文案。
     */
    List<WorkflowQueryVO> toTargetList(List<WorkflowQueryVO> sourceList);
}

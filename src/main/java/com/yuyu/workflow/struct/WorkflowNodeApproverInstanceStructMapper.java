package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.eto.workflow.*;
import org.mapstruct.Mapper;

/**
 * 流程定义对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface WorkflowNodeApproverInstanceStructMapper {


    WorkflowRejectAuditETO convertRejectDto(WorkflowAuditETO source);

}

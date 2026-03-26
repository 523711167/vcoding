package com.yuyu.workflow.convert;

import com.yuyu.workflow.common.mapstruct.BaseMapper;
import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.entity.*;
import com.yuyu.workflow.eto.workflow.*;
import com.yuyu.workflow.vo.workflow.WorkflowDefinitionVO;
import com.yuyu.workflow.vo.workflow.WorkflowNodeApproverVO;
import com.yuyu.workflow.vo.workflow.WorkflowNodeVO;
import com.yuyu.workflow.vo.workflow.WorkflowTransitionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Objects;

/**
 * 流程定义对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface WorkflowNodeApproverInstanceStructMapper {


    WorkflowRejectAuditETO convertRejectDto(WorkflowAuditETO source);

}

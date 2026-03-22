package com.yuyu.workflow.service;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionCreateETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionDisableETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionPublishETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionUpdateETO;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionPageQTO;
import com.yuyu.workflow.vo.workflow.WorkflowDefinitionVO;

import java.util.List;

/**
 * 流程定义服务接口。
 */
public interface WorkflowDefinitionService {

    WorkflowDefinitionVO create(WorkflowDefinitionCreateETO eto);

    WorkflowDefinitionVO update(WorkflowDefinitionUpdateETO eto);

    void delete(List<Long> idList);

    List<WorkflowDefinitionVO> list(WorkflowDefinitionListQTO qto);

    PageVo<WorkflowDefinitionVO> page(WorkflowDefinitionPageQTO qto);

    WorkflowDefinitionVO detail(Long id);

    void publish(WorkflowDefinitionPublishETO eto);

    void disable(WorkflowDefinitionDisableETO eto);
}

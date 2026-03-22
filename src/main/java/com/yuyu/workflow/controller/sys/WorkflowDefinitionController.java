package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.eto.base.BaseIdETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionCreateETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionDisableETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionPublishETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionUpdateETO;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionIdQTO;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionPageQTO;
import com.yuyu.workflow.service.WorkflowDefinitionService;
import com.yuyu.workflow.vo.workflow.WorkflowDefinitionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程定义管理控制器。
 */
@RestController
@Validated
@Tag(name = "流程定义管理")
@RequestMapping("/sys/workflow-definition")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;

    /**
     * 注入流程定义服务。
     */
    public WorkflowDefinitionController(WorkflowDefinitionService workflowDefinitionService) {
        this.workflowDefinitionService = workflowDefinitionService;
    }

    @Operation(summary = "新增流程定义")
    @PostMapping("/create")
    public Resp<WorkflowDefinitionVO> create(@Valid @RequestBody WorkflowDefinitionCreateETO eto) {
        return Resp.success(workflowDefinitionService.create(eto));
    }

    @Operation(summary = "修改流程定义")
    @PostMapping("/update")
    public Resp<WorkflowDefinitionVO> update(@Valid @RequestBody WorkflowDefinitionUpdateETO eto) {
        return Resp.success(workflowDefinitionService.update(eto));
    }

    @Operation(summary = "删除流程定义")
    @PostMapping("/delete")
    public Resp<Void> delete(@Valid @RequestBody BaseIdETO eto) {
        workflowDefinitionService.delete(resolveDeleteIds(eto));
        return Resp.success();
    }

    @Operation(summary = "查询流程定义列表")
    @GetMapping("/list")
    public Resp<List<WorkflowDefinitionVO>> list(@Valid @ParameterObject WorkflowDefinitionListQTO qto) {
        return Resp.success(workflowDefinitionService.list(qto));
    }

    @Operation(summary = "分页查询流程定义")
    @GetMapping("/page")
    public Resp<PageVo<WorkflowDefinitionVO>> page(@Valid @ParameterObject WorkflowDefinitionPageQTO qto) {
        return Resp.success(workflowDefinitionService.page(qto));
    }

    @Operation(summary = "查询流程定义详情")
    @GetMapping("/detail")
    public Resp<WorkflowDefinitionVO> detail(@Valid @ParameterObject WorkflowDefinitionIdQTO qto) {
        return Resp.success(workflowDefinitionService.detail(qto.getId()));
    }

    @Operation(summary = "发布流程定义")
    @PostMapping("/publish")
    public Resp<Void> publish(@Valid @RequestBody WorkflowDefinitionPublishETO eto) {
        workflowDefinitionService.publish(eto);
        return Resp.success();
    }

    @Operation(summary = "停用流程定义")
    @PostMapping("/disable")
    public Resp<Void> disable(@Valid @RequestBody WorkflowDefinitionDisableETO eto) {
        workflowDefinitionService.disable(eto);
        return Resp.success();
    }

    /**
     * 优先提取批量删除主键集合。
     */
    private List<Long> resolveDeleteIds(BaseIdETO eto) {
        if (!CollectionUtils.isEmpty(eto.getIdList())) {
            return eto.getIdList();
        }
        return List.of(eto.getId());
    }
}

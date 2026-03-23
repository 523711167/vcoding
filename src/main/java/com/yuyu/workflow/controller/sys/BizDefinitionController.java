package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.eto.base.BaseIdETO;
import com.yuyu.workflow.eto.biz.BizDefinitionCreateETO;
import com.yuyu.workflow.eto.biz.BizDefinitionUpdateETO;
import com.yuyu.workflow.qto.biz.BizDefinitionIdQTO;
import com.yuyu.workflow.qto.biz.BizDefinitionListQTO;
import com.yuyu.workflow.qto.biz.BizDefinitionPageQTO;
import com.yuyu.workflow.service.BizDefinitionService;
import com.yuyu.workflow.vo.biz.BizDefinitionVO;
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
 * 业务定义管理控制器。
 */
@RestController
@Validated
@Tag(name = "业务定义管理")
@RequestMapping("/sys/biz")
public class BizDefinitionController {

    private final BizDefinitionService bizDefinitionService;

    /**
     * 注入业务定义服务。
     */
    public BizDefinitionController(BizDefinitionService bizDefinitionService) {
        this.bizDefinitionService = bizDefinitionService;
    }

    /**
     * 新增业务定义。
     */
    @Operation(summary = "新增业务定义")
    @PostMapping("/create")
    public Resp<BizDefinitionVO> create(@Valid @RequestBody BizDefinitionCreateETO eto) {
        return Resp.success(bizDefinitionService.create(eto));
    }

    /**
     * 修改业务定义。
     */
    @Operation(summary = "修改业务定义")
    @PostMapping("/update")
    public Resp<BizDefinitionVO> update(@Valid @RequestBody BizDefinitionUpdateETO eto) {
        return Resp.success(bizDefinitionService.update(eto));
    }

    /**
     * 删除业务定义。
     */
    @Operation(summary = "删除业务定义")
    @PostMapping("/delete")
    public Resp<Void> delete(@Valid @RequestBody BaseIdETO eto) {
        bizDefinitionService.delete(resolveDeleteIds(eto));
        return Resp.success();
    }

    /**
     * 查询业务定义列表。
     */
    @Operation(summary = "查询业务定义列表")
    @GetMapping("/list")
    public Resp<List<BizDefinitionVO>> list(@Valid @ParameterObject BizDefinitionListQTO qto) {
        return Resp.success(bizDefinitionService.list(qto));
    }

    /**
     * 分页查询业务定义列表。
     */
    @Operation(summary = "分页查询业务定义列表")
    @GetMapping("/page")
    public Resp<PageVo<BizDefinitionVO>> page(@Valid @ParameterObject BizDefinitionPageQTO qto) {
        return Resp.success(bizDefinitionService.page(qto));
    }

    /**
     * 查询业务定义详情。
     */
    @Operation(summary = "查询业务定义详情")
    @GetMapping("/detail")
    public Resp<BizDefinitionVO> detail(@Valid @ParameterObject BizDefinitionIdQTO qto) {
        return Resp.success(bizDefinitionService.detail(qto.getId()));
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

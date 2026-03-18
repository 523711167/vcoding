package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.eto.base.BaseIdETO;
import com.yuyu.workflow.eto.dept.DeptCreateETO;
import com.yuyu.workflow.eto.dept.DeptMoveETO;
import com.yuyu.workflow.eto.dept.DeptUpdateETO;
import com.yuyu.workflow.qto.dept.DeptIdQTO;
import com.yuyu.workflow.qto.dept.DeptTreeQTO;
import com.yuyu.workflow.service.DeptService;
import com.yuyu.workflow.vo.dept.DeptTreeVO;
import com.yuyu.workflow.vo.dept.DeptVO;
import com.yuyu.workflow.vo.role.UserSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.CollectionUtils;

import java.util.List;

@RestController
@Validated
@Tag(name = "组织管理")
@RequestMapping("/sys/dept")
public class DeptController {

    private final DeptService deptService;

    /**
     * 注入部门服务。
     */
    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    /**
     * 新增部门。
     */
    @Operation(summary = "新增部门")
    @PostMapping("/create")
    public Resp<DeptVO> create(@Valid @RequestBody DeptCreateETO eto) {
        return Resp.success(deptService.create(eto));
    }

    /**
     * 修改部门。
     */
    @Operation(summary = "修改部门")
    @PostMapping("/update")
    public Resp<DeptVO> update(@Valid @RequestBody DeptUpdateETO eto) {
        return Resp.success(deptService.update(eto));
    }

    /**
     * 移动部门。
     */
    @Operation(summary = "移动部门")
    @PostMapping("/move")
    public Resp<Void> move(@Valid @RequestBody DeptMoveETO eto) {
        deptService.move(eto);
        return Resp.success();
    }

    /**
     * 删除部门。
     */
    @Operation(summary = "删除部门")
    @PostMapping("/delete")
    public Resp<Void> delete(@Valid @RequestBody BaseIdETO eto) {
        deptService.delete(resolveDeleteIds(eto));
        return Resp.success();
    }

    /**
     * 查询部门树。
     */
    @Operation(summary = "查询部门树")
    @GetMapping("/tree")
    public Resp<List<DeptTreeVO>> tree(@Valid @ParameterObject DeptTreeQTO qto) {
        return Resp.success(deptService.tree(qto));
    }

    /**
     * 查询部门详情。
     */
    @Operation(summary = "查询部门详情")
    @GetMapping("/detail")
    public Resp<DeptVO> detail(@Valid @ParameterObject DeptIdQTO qto) {
        return Resp.success(deptService.detail(qto.getId()));
    }

    /**
     * 查询部门下用户。
     */
    @Operation(summary = "查询部门下用户")
    @GetMapping("/users")
    public Resp<List<UserSimpleVO>> getUsers(@Valid @ParameterObject DeptIdQTO qto) {
        return Resp.success(deptService.getUsers(qto.getId()));
    }

    /**
     * 优先提取批量删除主键集合，单主键删除自动转换为单元素集合。
     */
    private List<Long> resolveDeleteIds(BaseIdETO eto) {
        if (!CollectionUtils.isEmpty(eto.getIdList())) {
            return eto.getIdList();
        }
        return List.of(eto.getId());
    }
}

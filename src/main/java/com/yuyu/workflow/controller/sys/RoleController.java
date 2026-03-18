package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.eto.base.BaseIdETO;
import com.yuyu.workflow.eto.role.RoleCreateETO;
import com.yuyu.workflow.eto.role.RoleDataScopeUpdateETO;
import com.yuyu.workflow.eto.role.RoleMenusUpdateETO;
import com.yuyu.workflow.eto.role.RoleStatusUpdateETO;
import com.yuyu.workflow.eto.role.RoleUpdateETO;
import com.yuyu.workflow.qto.role.RoleIdQTO;
import com.yuyu.workflow.qto.role.RoleListQTO;
import com.yuyu.workflow.qto.role.RolePageQTO;
import com.yuyu.workflow.vo.role.RoleMenuVO;
import com.yuyu.workflow.service.RoleService;
import com.yuyu.workflow.vo.role.RoleVO;
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
@Tag(name = "角色管理")
@RequestMapping("/sys/role")
public class RoleController {

    private final RoleService roleService;

    /**
     * 注入角色服务。
     */
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 新增角色。
     */
    @Operation(summary = "新增角色")
    @PostMapping("/create")
    public Resp<RoleVO> create(@Valid @RequestBody RoleCreateETO eto) {
        return Resp.success(roleService.create(eto));
    }

    /**
     * 修改角色。
     */
    @Operation(summary = "修改角色")
    @PostMapping("/update")
    public Resp<RoleVO> update(@Valid @RequestBody RoleUpdateETO eto) {
        return Resp.success(roleService.update(eto));
    }

    /**
     * 删除角色。
     */
    @Operation(summary = "删除角色")
    @PostMapping("/delete")
    public Resp<Void> delete(@Valid @RequestBody BaseIdETO eto) {
        roleService.delete(resolveDeleteIds(eto));
        return Resp.success();
    }

    /**
     * 查询角色列表。
     */
    @Operation(summary = "查询角色列表")
    @GetMapping("/list")
    public Resp<List<RoleVO>> list(@Valid @ParameterObject RoleListQTO qto) {
        return Resp.success(roleService.list(qto));
    }

    /**
     * 分页查询角色列表。
     */
    @Operation(summary = "分页查询角色列表")
    @GetMapping("/page")
    public Resp<PageVo<RoleVO>> page(@Valid @ParameterObject RolePageQTO qto) {
        return Resp.success(roleService.page(qto));
    }

    /**
     * 查询角色详情。
     */
    @Operation(summary = "查询角色详情")
    @GetMapping("/detail")
    public Resp<RoleVO> detail(@Valid @ParameterObject RoleIdQTO qto) {
        return Resp.success(roleService.detail(qto.getId()));
    }

    /**
     * 更新角色状态。
     */
    @Operation(summary = "更新角色状态")
    @PostMapping("/status/update")
    public Resp<Void> updateStatus(@Valid @RequestBody RoleStatusUpdateETO eto) {
        roleService.updateStatus(eto);
        return Resp.success();
    }

    /**
     * 全量更新角色菜单。
     */
    @Operation(summary = "全量更新角色菜单")
    @PostMapping("/menus/update")
    public Resp<Void> updateMenus(@Valid @RequestBody RoleMenusUpdateETO eto) {
        roleService.updateMenus(eto);
        return Resp.success();
    }

    /**
     * 查询角色已关联菜单。
     */
    @Operation(summary = "查询角色已关联菜单")
    @GetMapping("/menus")
    public Resp<RoleMenuVO> getMenus(@Valid @ParameterObject RoleIdQTO qto) {
        return Resp.success(roleService.getMenus(qto.getId()));
    }

    /**
     * 更新角色数据权限。
     */
    @Operation(summary = "更新角色数据权限")
    @PostMapping("/data-scope/update")
    public Resp<Void> updateDataScope(@Valid @RequestBody RoleDataScopeUpdateETO eto) {
        roleService.updateDataScope(eto);
        return Resp.success();
    }

    /**
     * 查询角色下用户。
     */
    @Operation(summary = "查询角色下用户")
    @GetMapping("/users")
    public Resp<List<UserSimpleVO>> getUsers(@Valid @ParameterObject RoleIdQTO qto) {
        return Resp.success(roleService.getUsers(qto.getId()));
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

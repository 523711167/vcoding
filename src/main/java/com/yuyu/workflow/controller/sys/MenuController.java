package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.eto.base.BaseIdETO;
import com.yuyu.workflow.eto.menu.MenuCreateETO;
import com.yuyu.workflow.eto.menu.MenuUpdateETO;
import com.yuyu.workflow.qto.menu.MenuIdQTO;
import com.yuyu.workflow.qto.menu.MenuTreeQTO;
import com.yuyu.workflow.service.MenuService;
import com.yuyu.workflow.vo.menu.MenuTreeVO;
import com.yuyu.workflow.vo.menu.MenuVO;
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
 * 菜单管理控制器。
 */
@RestController
@Validated
@Tag(name = "菜单管理")
@RequestMapping("/sys/menu")
public class MenuController {

    private final MenuService menuService;

    /**
     * 注入菜单服务。
     */
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 新增菜单。
     */
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public Resp<MenuVO> create(@Valid @RequestBody MenuCreateETO eto) {
        return Resp.success(menuService.create(eto));
    }

    /**
     * 修改菜单。
     */
    @Operation(summary = "修改菜单")
    @PostMapping("/update")
    public Resp<MenuVO> update(@Valid @RequestBody MenuUpdateETO eto) {
        return Resp.success(menuService.update(eto));
    }

    /**
     * 删除菜单。
     */
    @Operation(summary = "删除菜单")
    @PostMapping("/delete")
    public Resp<Void> delete(@Valid @RequestBody BaseIdETO eto) {
        menuService.delete(resolveDeleteIds(eto));
        return Resp.success();
    }

    /**
     * 查询菜单树。
     */
    @Operation(summary = "查询菜单树")
    @GetMapping("/tree")
    public Resp<List<MenuTreeVO>> tree(@Valid @ParameterObject MenuTreeQTO qto) {
        return Resp.success(menuService.tree(qto));
    }

    /**
     * 查询菜单详情。
     */
    @Operation(summary = "查询菜单详情")
    @GetMapping("/detail")
    public Resp<MenuVO> detail(@Valid @ParameterObject MenuIdQTO qto) {
        return Resp.success(menuService.detail(qto.getId()));
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

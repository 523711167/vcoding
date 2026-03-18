package com.yuyu.workflow.controller.sys;

import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.eto.base.BaseIdETO;
import com.yuyu.workflow.eto.user.UserCreateETO;
import com.yuyu.workflow.eto.user.UserDeptsUpdateETO;
import com.yuyu.workflow.eto.user.UserPasswordResetETO;
import com.yuyu.workflow.eto.user.UserRolesUpdateETO;
import com.yuyu.workflow.eto.user.UserStatusUpdateETO;
import com.yuyu.workflow.eto.user.UserUpdateETO;
import com.yuyu.workflow.qto.user.UserIdQTO;
import com.yuyu.workflow.qto.user.UserListQTO;
import com.yuyu.workflow.qto.user.UserPageQTO;
import com.yuyu.workflow.service.UserService;
import com.yuyu.workflow.vo.user.RoleSimpleVO;
import com.yuyu.workflow.vo.user.UserDeptVO;
import com.yuyu.workflow.vo.user.UserVO;
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
@Tag(name = "用户管理")
@RequestMapping("/sys/user")
public class UserController {

    private final UserService userService;

    /**
     * 注入用户服务。
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 新增用户。
     */
    @Operation(summary = "新增用户")
    @PostMapping("/create")
    public Resp<UserVO> create(@Valid @RequestBody UserCreateETO eto) {
        return Resp.success(userService.create(eto));
    }

    /**
     * 修改用户。
     */
    @Operation(summary = "修改用户")
    @PostMapping("/update")
    public Resp<UserVO> update(@Valid @RequestBody UserUpdateETO eto) {
        return Resp.success(userService.update(eto));
    }

    /**
     * 删除用户。
     */
    @Operation(summary = "删除用户")
    @PostMapping("/delete")
    public Resp<Void> delete(@Valid @RequestBody BaseIdETO eto) {
        userService.delete(resolveDeleteIds(eto));
        return Resp.success();
    }

    /**
     * 查询用户列表。
     */
    @Operation(summary = "查询用户列表")
    @GetMapping("/list")
    public Resp<List<UserVO>> list(@Valid @ParameterObject UserListQTO qto) {
        return Resp.success(userService.list(qto));
    }

    /**
     * 分页查询用户列表。
     */
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    public Resp<PageVo<UserVO>> page(@Valid @ParameterObject UserPageQTO qto) {
        return Resp.success(userService.page(qto));
    }

    /**
     * 查询用户详情。
     */
    @Operation(summary = "查询用户详情")
    @GetMapping("/detail")
    public Resp<UserVO> detail(@Valid @ParameterObject UserIdQTO qto) {
        return Resp.success(userService.detail(qto.getId()));
    }

    /**
     * 重置用户密码。
     */
    @Operation(summary = "重置用户密码")
    @PostMapping("/password/reset")
    public Resp<Void> resetPassword(@Valid @RequestBody UserPasswordResetETO eto) {
        userService.resetPassword(eto);
        return Resp.success();
    }

    /**
     * 更新用户状态。
     */
    @Operation(summary = "更新用户状态")
    @PostMapping("/status/update")
    public Resp<Void> updateStatus(@Valid @RequestBody UserStatusUpdateETO eto) {
        userService.updateStatus(eto);
        return Resp.success();
    }

    /**
     * 全量更新用户角色。
     */
    @Operation(summary = "全量更新用户角色")
    @PostMapping("/roles/update")
    public Resp<Void> updateRoles(@Valid @RequestBody UserRolesUpdateETO eto) {
        userService.updateRoles(eto);
        return Resp.success();
    }

    /**
     * 查询用户已关联角色。
     */
    @Operation(summary = "查询用户已关联角色")
    @GetMapping("/roles")
    public Resp<List<RoleSimpleVO>> getRoles(@Valid @ParameterObject UserIdQTO qto) {
        return Resp.success(userService.getRoles(qto.getId()));
    }

    /**
     * 全量更新用户组织。
     */
    @Operation(summary = "全量更新用户组织")
    @PostMapping("/depts/update")
    public Resp<Void> updateDepts(@Valid @RequestBody UserDeptsUpdateETO eto) {
        userService.updateDepts(eto);
        return Resp.success();
    }

    /**
     * 查询用户已关联组织。
     */
    @Operation(summary = "查询用户已关联组织")
    @GetMapping("/depts")
    public Resp<List<UserDeptVO>> getDepts(@Valid @ParameterObject UserIdQTO qto) {
        return Resp.success(userService.getDepts(qto.getId()));
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

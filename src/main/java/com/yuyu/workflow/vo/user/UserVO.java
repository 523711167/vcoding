package com.yuyu.workflow.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "用户返回对象")
public class UserVO {

    @Schema(description = "用户ID")
    private Long id;
    @Schema(description = "登录用户名")
    private String username;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "手机号")
    private String mobile;
    @Schema(description = "头像地址")
    private String avatar;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "状态说明")
    private String statusMsg;
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @Schema(description = "角色列表")
    private List<RoleSimpleVO> roles;
    @Schema(description = "组织列表")
    private List<UserDeptVO> depts;
}

package com.yuyu.workflow.common.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 携带当前登录用户上下文的参数基类。
 */
@Data
@Schema(description = "当前用户上下文参数基类")
public class UserContextParam {

    @Schema(description = "当前登录用户ID", hidden = true)
    private Long currentUserId;

    @Schema(description = "当前登录用户名", hidden = true)
    private String currentUsername;

    @Schema(description = "当前登录用户所属主组织ID", hidden = true)
    private Long currentPrimaryDeptId;


    public UserContextParam getUserContextParam() {
        return this;
    }
}

package com.yuyu.workflow.vo.biz;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户可查看业务定义返回对象。
 */
@Data
@Schema(description = "当前用户可查看业务定义返回对象")
public class BizDefinitionCurrentUserVO {

    @Schema(description = "业务定义ID")
    private Long id;

    @Schema(description = "业务编码")
    private String bizCode;

    @Schema(description = "业务名称")
    private String bizName;

    @Schema(description = "业务描述")
    private String bizDesc;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态说明")
    private String statusMsg;

    @Schema(description = "创建人用户ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

package com.yuyu.workflow.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "部门返回对象")
public class DeptVO {

    @Schema(description = "部门ID")
    private Long id;
    @Schema(description = "父部门ID")
    private Long parentId;
    @Schema(description = "部门名称")
    private String name;
    @Schema(description = "部门编码")
    private String code;
    @Schema(description = "组织类型")
    private String orgType;
    @Schema(description = "组织类型说明")
    private String orgTypeMsg;
    @Schema(description = "岗位类型")
    private String postType;
    @Schema(description = "组织路径")
    private String path;
    @Schema(description = "层级")
    private Integer level;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "主管用户ID")
    private Long leaderId;
    @Schema(description = "主管姓名")
    private String leaderName;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "状态说明")
    private String statusMsg;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

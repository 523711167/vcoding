package com.yuyu.workflow.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户组织信息")
public class UserDeptVO {

    @Schema(description = "组织ID")
    private Long id;
    @Schema(description = "父组织ID")
    private Long parentId;
    @Schema(description = "组织名称")
    private String name;
    @Schema(description = "组织编码")
    private String code;
    @Schema(description = "主管用户ID")
    private Long leaderId;
    @Schema(description = "主管姓名")
    private String leaderName;
    @Schema(description = "组织状态")
    private Integer status;
    @Schema(description = "是否主组织")
    private Integer isPrimary;
    @Schema(description = "是否主组织说明")
    private String isPrimaryMsg;
}

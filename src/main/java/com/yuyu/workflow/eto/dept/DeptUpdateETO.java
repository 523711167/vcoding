package com.yuyu.workflow.eto.dept;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.OrgTypeEnum;
import com.yuyu.workflow.common.validation.EnumIdValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改部门参数")
public class DeptUpdateETO {

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name不能为空")
    @Size(max = 100, message = "name长度不能超过100")
    private String name;

    @Schema(description = "同级唯一部门编码")
    @Size(max = 64, message = "code长度不能超过64")
    private String code;

    @Schema(description = "组织类型：GROUP/COMPANY/DEPT/POST", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "orgType不能为空")
    @Pattern(regexp = OrgTypeEnum.REGEXP, message = "orgType不合法")
    private String orgType;

    @Schema(description = "岗位类型，orgType=POST时必填")
    @Size(max = 64, message = "postType长度不能超过64")
    private String postType;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "部门主管用户ID")
    private Long leaderId;

    @Schema(description = "状态：1=正常 0=停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "status不能为空")
    @EnumIdValid(enumClass = CommonStatusEnum.class, allowNull = false, message = "status不合法")
    private Integer status;
}

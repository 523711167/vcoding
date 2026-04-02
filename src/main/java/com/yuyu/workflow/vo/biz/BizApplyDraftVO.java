package com.yuyu.workflow.vo.biz;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 业务申请草稿返回对象。
 */
@Data
@Schema(description = "业务申请草稿")
public class BizApplyDraftVO {

    @Schema(description = "业务申请ID", example = "1")
    private Long id;

    @Schema(description = "业务定义ID", example = "1")
    private Long bizDefinitionId;

    @Schema(description = "申请标题", example = "员工报销申请")
    private String title;

    @Schema(description = "业务申请状态", example = "DRAFT")
    private String bizStatus;

    @Schema(description = "申请人ID", example = "1")
    private Long applicantId;

    @Schema(description = "申请人姓名", example = "系统管理员")
    private String applicantName;

    @Schema(description = "所属组织ID", example = "2")
    private Long deptId;

    @Schema(description = "业务表单JSON", example = "{\"amount\":1000}")
    private String formData;

    @Schema(description = "绑定流程名称快照", example = "员工报销流程")
    private String workflowName;
}

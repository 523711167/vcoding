package com.yuyu.workflow.vo.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 提交业务申请并发起审批结果。
 */
@Data
@Schema(description = "提交业务申请并发起审批结果")
public class WorkflowBizSubmitVO {

    @Schema(description = "业务申请ID", example = "1")
    private Long bizApplyId;

    @Schema(description = "流程实例ID", example = "1001")
    private Long workflowInstanceId;

    @Schema(description = "当前运行节点")
    private CurrentNodeVO currentNode;

    /**
     * 当前运行节点视图对象。
     */
    @Data
    @Schema(description = "当前运行节点")
    public static class CurrentNodeVO {

        @Schema(description = "节点实例ID", example = "2001")
        private Long nodeInstanceId;

        @Schema(description = "节点定义ID", example = "11")
        private Long nodeId;

        @Schema(description = "节点名称", example = "直属领导审批")
        private String nodeName;

        @Schema(description = "节点类型", example = "APPROVAL")
        private String nodeType;

        @Schema(description = "节点状态", example = "ACTIVE")
        private String status;

        @Schema(description = "审批模式", example = "OR")
        private String approveMode;

        @Schema(description = "当前节点审核人列表")
        private List<ApproverVO> approverList = new ArrayList<>();
    }

    /**
     * 当前节点审核人视图对象。
     */
    @Data
    @Schema(description = "当前节点审核人")
    public static class ApproverVO {

        @Schema(description = "审核人ID", example = "2")
        private Long approverId;

        @Schema(description = "审核人姓名", example = "直属领导")
        private String approverName;

        @Schema(description = "审核状态", example = "PENDING")
        private String status;

        @Schema(description = "是否当前激活：0=否 1=是", example = "1")
        private Integer isActive;

        @Schema(description = "顺序值", example = "1")
        private Integer sortOrder;

        @Schema(description = "来源关系类型", example = "ORIGINAL")
        private String relationType;
    }
}

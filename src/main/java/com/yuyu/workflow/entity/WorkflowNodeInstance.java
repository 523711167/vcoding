package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 节点实例持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_node_instance")
public class WorkflowNodeInstance extends BaseAuditEntity {

    private Long instanceId;

    private Long definitionNodeId;

    private String definitionNodeName;

    private String definitionNodeType;

    private Long parallelBranchRootId;

    private String status;

    private String approveMode;

    private LocalDateTime activatedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime deadlineAt;

    private LocalDateTime remindAt;

    private Integer isReminded;

    private String remark;


    public boolean isSpawnParallelSplitNode() {
        return Objects.nonNull(parallelBranchRootId);
    }

    public static WorkflowNodeInstance toEnd() {
        WorkflowNodeInstance workflowNodeInstance = new WorkflowNodeInstance();
        workflowNodeInstance.setDefinitionNodeType(WorkflowNodeTypeEnum.END.getCode());
        workflowNodeInstance.setDefinitionNodeName(WorkflowNodeTypeEnum.END.getName());
        return workflowNodeInstance;
    }
}

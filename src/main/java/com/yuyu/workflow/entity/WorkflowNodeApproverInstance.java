package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 节点审批人实例持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_node_approver_instance")
public class WorkflowNodeApproverInstance extends BaseCreateEntity {

    private Long nodeInstanceId;

    private Long instanceId;

    private Long approverId;

    private String approverName;

    private String nodeName;

    private String nodeType;

    private String relationType;

    private Long sourceApproverInstanceId;

    private Integer sortOrder;

    private String status;

    private Integer isActive;

    private LocalDateTime finishedAt;

    private String comment;

    private Long delegateTo;

    private String delegateToName;

    private LocalDateTime updatedAt;
}

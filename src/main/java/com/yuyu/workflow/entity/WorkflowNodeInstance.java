package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 节点实例持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_node_instance")
public class WorkflowNodeInstance extends BaseAuditEntity {

    private Long instanceId;

    @TableField("node_id")
    private Long definitionNodeId;

    @TableField("node_name")
    private String definitionNodeName;

    @TableField("node_type")
    private String definitionNodeType;

    private String status;

    private String approveMode;

    private LocalDateTime activatedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime deadlineAt;

    private LocalDateTime remindAt;

    private Integer isReminded;

    private String remark;
}

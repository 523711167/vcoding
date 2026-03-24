package com.yuyu.workflow.entity;

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

    private Long nodeId;

    private String nodeName;

    private String nodeType;

    private String status;

    private String approveMode;

    private LocalDateTime activatedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime deadlineAt;

    private LocalDateTime remindAt;

    private Integer isReminded;

    private String remark;
}

package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程实例持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_instance")
public class WorkflowInstance extends BaseAuditEntity {

    private Long bizId;

    private Long definitionId;

    private String definitionCode;

    private String title;

    private String status;

    private Long applicantId;

    private String applicantName;

    private String formData;

    private Long currentNodeId;

    private String currentNodeName;

    private String currentNodeType;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}

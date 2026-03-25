package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseIdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批操作记录持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_approval_record")
public class WorkflowApprovalRecord extends BaseIdEntity {

    private Long instanceId;

    private Long nodeInstanceId;

    private Long operatorId;

    private String operatorName;

    private String action;

    private String nodeInstanceType;

    private String nodeInstanceName;

    private String comment;

    private Long fromNodeId;

    private String fromNodeType;

    private String fromNodeName;

    private Long toNodeId;

    private String toNodeType;

    private String toNodeName;

    private String extraData;

    private LocalDateTime operatedAt;
}

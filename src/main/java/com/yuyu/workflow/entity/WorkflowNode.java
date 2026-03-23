package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程节点持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_node")
public class WorkflowNode extends BaseAuditEntity {

    private Long definitionId;

    private String name;

    private String nodeType;

    private String approveMode;

    private Integer timeoutMinutes;

    private String timeoutAction;

    private Integer remindMinutes;

    private Integer positionX;

    private Integer positionY;

    private String configJson;
}

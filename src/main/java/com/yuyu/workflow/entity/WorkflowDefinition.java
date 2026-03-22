package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程定义持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_definition")
public class WorkflowDefinition extends BaseAuditEntity {

    private String name;

    private String code;

    private Integer version;

    private String description;

    private Integer status;

    private String bizCode;

    private Long createdBy;
}

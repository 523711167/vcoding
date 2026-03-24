package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程连线持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_transition")
public class WorkflowTransition extends BaseCreateEntity {

    private Long definitionId;

    private Long fromNodeId;

    private Long toNodeId;

    private String conditionExpr;

    private Integer isDefault;

    private Integer priority;

    private String label;

    @TableLogic
    private Integer isDeleted;
}

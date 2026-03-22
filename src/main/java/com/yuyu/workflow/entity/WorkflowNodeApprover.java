package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点审批人配置持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_node_approver")
public class WorkflowNodeApprover extends BaseCreateEntity {

    private Long nodeId;

    private String approverType;

    private String approverValue;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;
}

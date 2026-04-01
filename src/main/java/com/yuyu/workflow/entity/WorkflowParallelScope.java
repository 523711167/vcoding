package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程并行作用域持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_parallel_scope")
public class WorkflowParallelScope extends BaseAuditEntity {

    /**
     * 流程实例ID。
     */
    private Long instanceId;

    /**
     * 流程定义ID。
     */
    private Long definitionId;

    /**
     * 并行拆分节点定义ID。
     */
    private Long splitDefinitionNodeId;

    /**
     * 并行拆分节点定义名称。
     */
    private String splitDefinitionNodeName;

    /**
     * 并行拆分节点定义类型。
     */
    private String splitDefinitionNodeType;

    /**
     * 并行聚合节点定义ID。
     */
    private Long joinDefinitionNodeId;

    /**
     * 并行聚合节点定义名称。
     */
    private String joinDefinitionNodeName;

    /**
     * 并行聚合节点定义类型。
     */
    private String joinDefinitionNodeType;

    /**
     * 父并行作用域ID。
     */
    private Long parentScopeId;

    /**
     * 作用域状态。
     */
    private String status;

    /**
     * 理论应汇聚分支数。
     */
    private Integer expectedBranchCount;

    /**
     * 当前已到达聚合节点的分支数。
     */
    private Integer arrivedBranchCount;

    /**
     * 作用域完成时间。
     */
    private LocalDateTime finishedAt;
}

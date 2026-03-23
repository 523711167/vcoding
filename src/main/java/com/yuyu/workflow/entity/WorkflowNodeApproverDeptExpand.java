package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流节点审批组织展开关系持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_workflow_node_approver_dept_expand")
public class WorkflowNodeApproverDeptExpand extends BaseCreateEntity {

    /**
     * 来源审批人配置ID。
     */
    private Long approverId;

    /**
     * 来源流程节点ID。
     */
    private Long nodeId;

    /**
     * 来源流程定义ID。
     */
    private Long definitionId;

    /**
     * 来源直接配置组织ID。
     */
    private Long sourceDeptId;

    /**
     * 来源组织类型。
     */
    private String sourceOrgType;

    /**
     * 来源岗位类型。
     */
    private String sourcePostType;

    /**
     * 展开后的目标组织ID。
     */
    private Long deptId;

    /**
     * 目标组织类型。
     */
    private String orgType;

    /**
     * 目标岗位类型。
     */
    private String postType;

    /**
     * 关系类型。
     */
    private String relationType;

    /**
     * 展开距离。
     */
    private Integer distance;
}

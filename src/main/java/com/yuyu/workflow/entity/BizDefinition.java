package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务定义持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_biz_definition")
public class BizDefinition extends BaseAuditEntity {

    /**
     * 业务编码。
     */
    private String bizCode;

    /**
     * 业务名称。
     */
    private String bizName;

    /**
     * 业务描述。
     */
    private String bizDesc;

    /**
     * 绑定的流程定义ID。
     */
    private Long workflowDefinitionId;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 创建人用户ID。
     */
    private Long createdBy;
}

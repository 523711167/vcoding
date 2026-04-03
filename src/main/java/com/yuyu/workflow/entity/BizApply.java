package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 业务申请持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_biz_apply")
public class BizApply extends BaseAuditEntity {

    private Long bizDefinitionId;

    private String bizName;

    private String title;

    private String bizStatus;

    private Long applicantId;

    private String applicantName;

    private Long deptId;

    private String formData;

    private String workflowName;

    private Long workflowInstanceId;

    private String cancelReason;

    private LocalDateTime submittedAt;

    private LocalDateTime finishedAt;
}

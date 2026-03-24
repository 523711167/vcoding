-- 变更原因：
-- 1. 工作流连线增加显式默认分支字段 is_default。
-- 2. 支持 CONDITION / PARALLEL_SPLIT 多分支时按默认分支规则做发布与保存校验。
-- 3. 按设计文档落地业务申请表和工作流运行层五张核心表。
-- 3. 落地业务申请与工作流运行层五张核心表。

USE `yuyu`;

SET @add_workflow_transition_is_default_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_transition'
              AND COLUMN_NAME = 'is_default'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_transition` ADD COLUMN `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否默认分支：0=否 1=是'' AFTER `condition_expr`'
    )
);

PREPARE stmt_add_workflow_transition_is_default FROM @add_workflow_transition_is_default_sql;
EXECUTE stmt_add_workflow_transition_is_default;
DEALLOCATE PREPARE stmt_add_workflow_transition_is_default;

CREATE TABLE IF NOT EXISTS `tb_biz_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '业务编码：LEAVE=请假 EXPENSE=报销 CONTRACT=合同 等',
  `title` VARCHAR(200) NOT NULL COMMENT '申请标题',
  `biz_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '业务申请状态：DRAFT=草稿 PENDING=审批中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `applicant_name` VARCHAR(64) NOT NULL COMMENT '申请人姓名（冗余）',
  `dept_id` BIGINT DEFAULT NULL COMMENT '申请人所属部门ID（冗余）',
  `form_data` JSON DEFAULT NULL COMMENT '业务差异化字段（按biz_code存储申请数据快照）',
  `workflow_name` VARCHAR(100) DEFAULT NULL COMMENT '流程名称（冗余快照，取提交流程定义名称）',
  `workflow_instance_id` BIGINT DEFAULT NULL COMMENT '关联的审批工作流实例ID（tb_workflow_instance.id），提交后回写',
  `submitted_at` DATETIME DEFAULT NULL COMMENT '提交审批时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '审批完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_biz_code_biz_status` (`biz_code`, `biz_status`),
  KEY `idx_workflow_instance_id` (`workflow_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用业务申请表';

CREATE TABLE IF NOT EXISTS `tb_workflow_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `definition_code` VARCHAR(64) NOT NULL COMMENT '流程编码（冗余，方便查询）',
  `title` VARCHAR(200) NOT NULL COMMENT '审批标题',
  `status` VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT '实例状态：RUNNING=进行中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `applicant_name` VARCHAR(64) NOT NULL COMMENT '申请人姓名（冗余）',
  `form_data` JSON DEFAULT NULL COMMENT '业务申请数据快照',
  `current_node_id` BIGINT DEFAULT NULL COMMENT '当前所在节点ID',
  `started_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` BIGINT NOT NULL COMMENT '所属流程实例ID',
  `node_id` BIGINT NOT NULL COMMENT '对应的节点定义ID',
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点名称（冗余）',
  `node_type` VARCHAR(32) NOT NULL COMMENT '节点类型（冗余）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待激活 ACTIVE=进行中 APPROVED=已通过 REJECTED=已拒绝 SKIPPED=已跳过 TIMEOUT=已超时',
  `approve_mode` VARCHAR(16) DEFAULT NULL COMMENT '审批模式（冗余）',
  `activated_at` DATETIME DEFAULT NULL COMMENT '节点激活时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '节点完成时间',
  `deadline_at` DATETIME DEFAULT NULL COMMENT '超时截止时间（activated_at + timeout_minutes）',
  `remind_at` DATETIME DEFAULT NULL COMMENT '催办提醒时间',
  `is_reminded` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送催办通知',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '节点备注（如自动处理原因）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_status_deadline` (`status`, `deadline_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_approver_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_instance_id` BIGINT NOT NULL COMMENT '所属节点实例ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID（冗余，方便查询）',
  `approver_id` BIGINT NOT NULL COMMENT '审批人用户ID',
  `approver_name` VARCHAR(64) NOT NULL COMMENT '审批人姓名（冗余）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '顺签顺序',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待处理 APPROVED=已通过 REJECTED=已拒绝 DELEGATED=已转交 CANCELED=已取消',
  `is_active` TINYINT NOT NULL DEFAULT 0 COMMENT '是否当前需要操作（顺签场景下只有当前人为1）',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `delegate_to` BIGINT DEFAULT NULL COMMENT '转交目标用户ID（DELEGATED状态时有值）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_node_instance_id` (`node_instance_id`),
  KEY `idx_approver_id_status` (`approver_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点审批人实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_approval_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
  `node_instance_id` BIGINT NOT NULL COMMENT '节点实例ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人用户ID',
  `operator_name` VARCHAR(64) NOT NULL COMMENT '操作人姓名',
  `action` VARCHAR(16) NOT NULL COMMENT '操作类型：SUBMIT=提交 APPROVE=通过 REJECT=拒绝 DELEGATE=转交 RECALL=撤回 URGE=催办 TIMEOUT=超时自动',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '操作备注',
  `from_node_id` BIGINT DEFAULT NULL COMMENT '操作时所在节点ID',
  `to_node_id` BIGINT DEFAULT NULL COMMENT '流转到的节点ID（通过时有值）',
  `extra_data` JSON DEFAULT NULL COMMENT '附加数据（如附件列表、转交目标等）',
  `operated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作记录表';

CREATE TABLE IF NOT EXISTS `tb_biz_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '业务编码：LEAVE=请假 EXPENSE=报销 CONTRACT=合同 等',
  `title` VARCHAR(200) NOT NULL COMMENT '申请标题',
  `biz_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '业务申请状态：DRAFT=草稿 PENDING=审批中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `applicant_name` VARCHAR(64) NOT NULL COMMENT '申请人姓名（冗余）',
  `dept_id` BIGINT DEFAULT NULL COMMENT '申请人所属部门ID（冗余）',
  `form_data` JSON DEFAULT NULL COMMENT '业务差异化字段（按biz_code存储申请数据快照）',
  `workflow_name` VARCHAR(100) DEFAULT NULL COMMENT '流程名称（冗余快照，取提交流程定义名称）',
  `workflow_instance_id` BIGINT DEFAULT NULL COMMENT '关联的审批工作流实例ID（tb_workflow_instance.id），提交后回写',
  `submitted_at` DATETIME DEFAULT NULL COMMENT '提交审批时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '审批完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_biz_code_biz_status` (`biz_code`, `biz_status`),
  KEY `idx_workflow_instance_id` (`workflow_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用业务申请表';

CREATE TABLE IF NOT EXISTS `tb_workflow_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `definition_code` VARCHAR(64) NOT NULL COMMENT '流程编码（冗余，方便查询）',
  `title` VARCHAR(200) NOT NULL COMMENT '审批标题',
  `status` VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT '实例状态：RUNNING=进行中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `applicant_name` VARCHAR(64) NOT NULL COMMENT '申请人姓名（冗余）',
  `form_data` JSON DEFAULT NULL COMMENT '业务申请数据快照',
  `current_node_id` BIGINT DEFAULT NULL COMMENT '当前所在节点ID',
  `started_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` BIGINT NOT NULL COMMENT '所属流程实例ID',
  `node_id` BIGINT NOT NULL COMMENT '对应的节点定义ID',
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点名称（冗余）',
  `node_type` VARCHAR(32) NOT NULL COMMENT '节点类型（冗余）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待激活 ACTIVE=进行中 APPROVED=已通过 REJECTED=已拒绝 SKIPPED=已跳过 TIMEOUT=已超时',
  `approve_mode` VARCHAR(16) DEFAULT NULL COMMENT '审批模式（冗余）',
  `activated_at` DATETIME DEFAULT NULL COMMENT '节点激活时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '节点完成时间',
  `deadline_at` DATETIME DEFAULT NULL COMMENT '超时截止时间（activated_at + timeout_minutes）',
  `remind_at` DATETIME DEFAULT NULL COMMENT '催办提醒时间',
  `is_reminded` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送催办通知',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '节点备注（如自动处理原因）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_status_deadline` (`status`, `deadline_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_approver_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_instance_id` BIGINT NOT NULL COMMENT '所属节点实例ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID（冗余，方便查询）',
  `approver_id` BIGINT NOT NULL COMMENT '审批人用户ID',
  `approver_name` VARCHAR(64) NOT NULL COMMENT '审批人姓名（冗余）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '顺签顺序',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待处理 APPROVED=已通过 REJECTED=已拒绝 DELEGATED=已转交 CANCELED=已取消',
  `is_active` TINYINT NOT NULL DEFAULT 0 COMMENT '是否当前需要操作（顺签场景下只有当前人为1）',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `delegate_to` BIGINT DEFAULT NULL COMMENT '转交目标用户ID（DELEGATED状态时有值）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_node_instance_id` (`node_instance_id`),
  KEY `idx_approver_id_status` (`approver_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点审批人实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_approval_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
  `node_instance_id` BIGINT NOT NULL COMMENT '节点实例ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人用户ID',
  `operator_name` VARCHAR(64) NOT NULL COMMENT '操作人姓名',
  `action` VARCHAR(16) NOT NULL COMMENT '操作类型：SUBMIT=提交 APPROVE=通过 REJECT=拒绝 DELEGATE=转交 RECALL=撤回 URGE=催办 TIMEOUT=超时自动',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '操作备注',
  `from_node_id` BIGINT DEFAULT NULL COMMENT '操作时所在节点ID',
  `to_node_id` BIGINT DEFAULT NULL COMMENT '流转到的节点ID（通过时有值）',
  `extra_data` JSON DEFAULT NULL COMMENT '附加数据（如附件列表、转交目标等）',
  `operated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作记录表';

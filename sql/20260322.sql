-- 变更原因：
-- 1. 按工作流设计文档落地流程定义层四张核心表。
-- 2. 提供流程定义、节点、审批人配置、流转连线的基础持久化结构。

USE `yuyu`;

CREATE TABLE IF NOT EXISTS `tb_workflow_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '流程名称',
  `code` VARCHAR(64) NOT NULL COMMENT '流程编码（唯一标识）',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号，从1开始递增',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '流程描述',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=已发布 2=已停用',
  `biz_code` VARCHAR(128) DEFAULT NULL COMMENT '流程绑定业务编码，值与 tb_biz_definition.biz_code 保持一致',
  `created_by` BIGINT NOT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_version` (`code`, `version`),
  KEY `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `definition_id` BIGINT NOT NULL COMMENT '所属流程定义ID',
  `code` VARCHAR(64) NOT NULL COMMENT '节点编码（定义内唯一）',
  `name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `node_type` VARCHAR(32) NOT NULL COMMENT '节点类型：START/APPROVAL/CONDITION/PARALLEL_SPLIT/PARALLEL_JOIN/END',
  `approve_mode` VARCHAR(16) DEFAULT NULL COMMENT '多人审批模式：AND/OR/SEQUENTIAL（仅APPROVAL节点有效）',
  `timeout_hours` INT DEFAULT NULL COMMENT '超时时限（小时）',
  `timeout_action` VARCHAR(16) DEFAULT NULL COMMENT '超时处理策略：AUTO_APPROVE/AUTO_REJECT/NOTIFY_ONLY',
  `remind_hours` INT DEFAULT NULL COMMENT '提醒时限（小时）',
  `position_x` INT NOT NULL DEFAULT 0 COMMENT '节点在画布上的X坐标',
  `position_y` INT NOT NULL DEFAULT 0 COMMENT '节点在画布上的Y坐标',
  `config_json` JSON DEFAULT NULL COMMENT '节点扩展配置JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_definition_code` (`definition_id`, `code`),
  KEY `idx_definition_id` (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_approver` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_id` BIGINT NOT NULL COMMENT '所属节点ID',
  `approver_type` VARCHAR(16) NOT NULL COMMENT '审批人类型：USER/ROLE/DEPT/INITIATOR_DEPT_LEADER',
  `approver_value` VARCHAR(256) NOT NULL COMMENT '审批人值：用户ID/角色ID/组织ID',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '顺序值（顺签场景有效）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点审批人配置表';

CREATE TABLE IF NOT EXISTS `tb_workflow_transition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `definition_id` BIGINT NOT NULL COMMENT '所属流程定义ID',
  `from_node_id` BIGINT NOT NULL COMMENT '来源节点ID',
  `to_node_id` BIGINT NOT NULL COMMENT '目标节点ID',
  `condition_expr` VARCHAR(512) DEFAULT NULL COMMENT '条件表达式（NULL表示无条件流转）',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '条件优先级（值越小越先判断）',
  `label` VARCHAR(64) DEFAULT NULL COMMENT '连线标签',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_definition_from` (`definition_id`, `from_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流转条件表';

-- 变更原因：
-- 1. 工作流连线增加显式默认分支字段 is_default。
-- 2. 支持 CONDITION / PARALLEL_SPLIT 多分支时按默认分支规则做发布与保存校验。
-- 3. 按设计文档落地业务申请表和工作流运行层五张核心表。
-- 4. 审批操作记录增加节点实例、来源节点、目标节点的类型和名称冗余字段。
-- 5. 对齐运行层状态与动作口径，并为节点审批人实例补充节点名称、节点类型冗余字段。
-- 6. 节点审批人实例增加来源关系字段，支持标识原始审批人与加签审批人链路。

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

SET @drop_workflow_approval_record_node_type_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_approval_record'
              AND COLUMN_NAME = 'node_type'
        ),
        'ALTER TABLE `tb_workflow_approval_record` DROP COLUMN `node_type`',
        'SELECT 1'
    )
);

PREPARE stmt_drop_workflow_approval_record_node_type FROM @drop_workflow_approval_record_node_type_sql;
EXECUTE stmt_drop_workflow_approval_record_node_type;
DEALLOCATE PREPARE stmt_drop_workflow_approval_record_node_type;

SET @drop_workflow_approval_record_node_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_approval_record'
              AND COLUMN_NAME = 'node_name'
        ),
        'ALTER TABLE `tb_workflow_approval_record` DROP COLUMN `node_name`',
        'SELECT 1'
    )
);

PREPARE stmt_drop_workflow_approval_record_node_name FROM @drop_workflow_approval_record_node_name_sql;
EXECUTE stmt_drop_workflow_approval_record_node_name;
DEALLOCATE PREPARE stmt_drop_workflow_approval_record_node_name;

SET @add_workflow_approval_record_node_instance_type_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_approval_record'
              AND COLUMN_NAME = 'node_instance_type'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_approval_record` ADD COLUMN `node_instance_type` VARCHAR(32) NULL COMMENT ''节点实例类型（冗余）'' AFTER `action`'
    )
);

PREPARE stmt_add_workflow_approval_record_node_instance_type FROM @add_workflow_approval_record_node_instance_type_sql;
EXECUTE stmt_add_workflow_approval_record_node_instance_type;
DEALLOCATE PREPARE stmt_add_workflow_approval_record_node_instance_type;

SET @add_workflow_approval_record_node_instance_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_approval_record'
              AND COLUMN_NAME = 'node_instance_name'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_approval_record` ADD COLUMN `node_instance_name` VARCHAR(100) NULL COMMENT ''节点实例名称（冗余）'' AFTER `node_instance_type`'
    )
);

PREPARE stmt_add_workflow_approval_record_node_instance_name FROM @add_workflow_approval_record_node_instance_name_sql;
EXECUTE stmt_add_workflow_approval_record_node_instance_name;
DEALLOCATE PREPARE stmt_add_workflow_approval_record_node_instance_name;

SET @add_workflow_approval_record_from_node_type_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_approval_record'
              AND COLUMN_NAME = 'from_node_type'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_approval_record` ADD COLUMN `from_node_type` VARCHAR(32) NULL COMMENT ''来源节点实例类型（冗余）'' AFTER `from_node_id`'
    )
);

PREPARE stmt_add_workflow_approval_record_from_node_type FROM @add_workflow_approval_record_from_node_type_sql;
EXECUTE stmt_add_workflow_approval_record_from_node_type;
DEALLOCATE PREPARE stmt_add_workflow_approval_record_from_node_type;

SET @add_workflow_approval_record_from_node_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_approval_record'
              AND COLUMN_NAME = 'from_node_name'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_approval_record` ADD COLUMN `from_node_name` VARCHAR(100) NULL COMMENT ''来源节点实例名称（冗余）'' AFTER `from_node_type`'
    )
);

PREPARE stmt_add_workflow_approval_record_from_node_name FROM @add_workflow_approval_record_from_node_name_sql;
EXECUTE stmt_add_workflow_approval_record_from_node_name;
DEALLOCATE PREPARE stmt_add_workflow_approval_record_from_node_name;

SET @add_workflow_approval_record_to_node_type_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_approval_record'
              AND COLUMN_NAME = 'to_node_type'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_approval_record` ADD COLUMN `to_node_type` VARCHAR(32) NULL COMMENT ''目标节点实例类型（冗余）'' AFTER `to_node_id`'
    )
);

PREPARE stmt_add_workflow_approval_record_to_node_type FROM @add_workflow_approval_record_to_node_type_sql;
EXECUTE stmt_add_workflow_approval_record_to_node_type;
DEALLOCATE PREPARE stmt_add_workflow_approval_record_to_node_type;

SET @add_workflow_approval_record_to_node_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_approval_record'
              AND COLUMN_NAME = 'to_node_name'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_approval_record` ADD COLUMN `to_node_name` VARCHAR(100) NULL COMMENT ''目标节点实例名称（冗余）'' AFTER `to_node_type`'
    )
);

PREPARE stmt_add_workflow_approval_record_to_node_name FROM @add_workflow_approval_record_to_node_name_sql;
EXECUTE stmt_add_workflow_approval_record_to_node_name;
DEALLOCATE PREPARE stmt_add_workflow_approval_record_to_node_name;

SET @add_workflow_node_approver_instance_node_name_sql = (SELECT IF(
                                                                         EXISTS(SELECT 1
                                                                                FROM information_schema.COLUMNS
                                                                                WHERE TABLE_SCHEMA = DATABASE()
                                                                                  AND TABLE_NAME = 'tb_workflow_node_approver_instance'
                                                                                  AND COLUMN_NAME = 'node_name'),
                                                                         'SELECT 1',
                                                                         'ALTER TABLE `tb_workflow_node_approver_instance` ADD COLUMN `node_name` VARCHAR(100) NOT NULL DEFAULT '''' COMMENT ''所属节点名称（冗余）'' AFTER `approver_name`'
                                                                 ));

PREPARE stmt_add_workflow_node_approver_instance_node_name FROM @add_workflow_node_approver_instance_node_name_sql;
EXECUTE stmt_add_workflow_node_approver_instance_node_name;
DEALLOCATE PREPARE stmt_add_workflow_node_approver_instance_node_name;

SET @add_workflow_node_approver_instance_node_type_sql = (SELECT IF(
                                                                         EXISTS(SELECT 1
                                                                                FROM information_schema.COLUMNS
                                                                                WHERE TABLE_SCHEMA = DATABASE()
                                                                                  AND TABLE_NAME = 'tb_workflow_node_approver_instance'
                                                                                  AND COLUMN_NAME = 'node_type'),
                                                                         'SELECT 1',
                                                                         'ALTER TABLE `tb_workflow_node_approver_instance` ADD COLUMN `node_type` VARCHAR(32) NOT NULL DEFAULT '''' COMMENT ''所属节点类型（冗余）'' AFTER `node_name`'
                                                                 ));

PREPARE stmt_add_workflow_node_approver_instance_node_type FROM @add_workflow_node_approver_instance_node_type_sql;
EXECUTE stmt_add_workflow_node_approver_instance_node_type;
DEALLOCATE PREPARE stmt_add_workflow_node_approver_instance_node_type;

SET @add_workflow_node_approver_instance_relation_type_sql = (SELECT IF(
                                                                             EXISTS(SELECT 1
                                                                                    FROM information_schema.COLUMNS
                                                                                    WHERE TABLE_SCHEMA = DATABASE()
                                                                                      AND TABLE_NAME = 'tb_workflow_node_approver_instance'
                                                                                      AND COLUMN_NAME = 'relation_type'),
                                                                             'SELECT 1',
                                                                             'ALTER TABLE `tb_workflow_node_approver_instance` ADD COLUMN `relation_type` VARCHAR(16) NOT NULL DEFAULT ''ORIGINAL'' COMMENT ''来源关系类型：ORIGINAL=原始审批人 ADD_SIGN=加签审批人'' AFTER `node_type`'
                                                                     ));

PREPARE stmt_add_workflow_node_approver_instance_relation_type FROM @add_workflow_node_approver_instance_relation_type_sql;
EXECUTE stmt_add_workflow_node_approver_instance_relation_type;
DEALLOCATE PREPARE stmt_add_workflow_node_approver_instance_relation_type;

SET @add_wf_node_appr_inst_src_id_sql = (SELECT IF(
                                                        EXISTS(SELECT 1
                                                               FROM information_schema.COLUMNS
                                                               WHERE TABLE_SCHEMA = DATABASE()
                                                                 AND TABLE_NAME = 'tb_workflow_node_approver_instance'
                                                                 AND COLUMN_NAME = 'source_approver_instance_id'),
                                                        'SELECT 1',
                                                        'ALTER TABLE `tb_workflow_node_approver_instance` ADD COLUMN `source_approver_instance_id` BIGINT NULL COMMENT ''来源审批人实例ID（加签审批人时指向发起加签的审批人实例）'' AFTER `relation_type`'
                                                ));

PREPARE stmt_add_wf_node_appr_inst_src_id FROM @add_wf_node_appr_inst_src_id_sql;
EXECUTE stmt_add_wf_node_appr_inst_src_id;
DEALLOCATE PREPARE stmt_add_wf_node_appr_inst_src_id;

SET @modify_workflow_node_instance_status_sql = (SELECT IF(
                                                                EXISTS(SELECT 1
                                                                       FROM information_schema.COLUMNS
                                                                       WHERE TABLE_SCHEMA = DATABASE()
                                                                         AND TABLE_NAME = 'tb_workflow_node_instance'
                                                                         AND COLUMN_NAME = 'status'),
                                                                'ALTER TABLE `tb_workflow_node_instance` MODIFY COLUMN `status` VARCHAR(16) NOT NULL DEFAULT ''PENDING'' COMMENT ''状态：PENDING=待激活 ACTIVE=进行中 APPROVED=已通过 REJECTED=已拒绝 SKIPPED=已跳过 CANCELED=已取消 TIMEOUT=已超时''',
                                                                'SELECT 1'
                                                        ));

PREPARE stmt_modify_workflow_node_instance_status FROM @modify_workflow_node_instance_status_sql;
EXECUTE stmt_modify_workflow_node_instance_status;
DEALLOCATE PREPARE stmt_modify_workflow_node_instance_status;

SET @modify_workflow_node_approver_instance_status_sql = (SELECT IF(
                                                                         EXISTS(SELECT 1
                                                                                FROM information_schema.COLUMNS
                                                                                WHERE TABLE_SCHEMA = DATABASE()
                                                                                  AND TABLE_NAME = 'tb_workflow_node_approver_instance'
                                                                                  AND COLUMN_NAME = 'status'),
                                                                         'ALTER TABLE `tb_workflow_node_approver_instance` MODIFY COLUMN `status` VARCHAR(16) NOT NULL DEFAULT ''PENDING'' COMMENT ''状态：PENDING=待处理 WAITING_ADD_SIGN=等待加签 APPROVED=已通过 REJECTED=已拒绝 DELEGATED=已转交 CANCELED=已取消''',
                                                                         'SELECT 1'
                                                                 ));

PREPARE stmt_modify_workflow_node_approver_instance_status FROM @modify_workflow_node_approver_instance_status_sql;
EXECUTE stmt_modify_workflow_node_approver_instance_status;
DEALLOCATE PREPARE stmt_modify_workflow_node_approver_instance_status;

SET @modify_workflow_approval_record_action_sql = (SELECT IF(
                                                                  EXISTS(SELECT 1
                                                                         FROM information_schema.COLUMNS
                                                                         WHERE TABLE_SCHEMA = DATABASE()
                                                                           AND TABLE_NAME = 'tb_workflow_approval_record'
                                                                           AND COLUMN_NAME = 'action'),
                                                                  'ALTER TABLE `tb_workflow_approval_record` MODIFY COLUMN `action` VARCHAR(16) NOT NULL COMMENT ''操作类型：SUBMIT=提交申请 APPROVE=审批通过 REJECT=审批拒绝 DELEGATE=审批转交 RECALL=发起人撤回 ADD_SIGN=发起加签 ROUTE=系统自动路由 SPLIT_TRIGGER=系统触发并行拆分 JOIN_ARRIVE=分支到达并行聚合节点 JOIN_PASS=并行聚合完成并继续流转 AUTO_APPROVE=系统自动审核通过 AUTO_REJECT=系统自动审批拒绝 TIMEOUT=节点超时自动处理触发记录 REMIND=节点超时后发送提醒''',
                                                                  'SELECT 1'
                                                          ));

PREPARE stmt_modify_workflow_approval_record_action FROM @modify_workflow_approval_record_action_sql;
EXECUTE stmt_modify_workflow_approval_record_action;
DEALLOCATE PREPARE stmt_modify_workflow_approval_record_action;

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
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待激活 ACTIVE=进行中 APPROVED=已通过 REJECTED=已拒绝 SKIPPED=已跳过 CANCELED=已取消 TIMEOUT=已超时',
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
  `node_name`                   VARCHAR(100) NOT NULL COMMENT '所属节点名称（冗余）',
  `node_type`                   VARCHAR(32)  NOT NULL COMMENT '所属节点类型（冗余）',
  `relation_type`               VARCHAR(16)  NOT NULL DEFAULT 'ORIGINAL' COMMENT '来源关系类型：ORIGINAL=原始审批人 ADD_SIGN=加签审批人',
  `source_approver_instance_id` BIGINT                DEFAULT NULL COMMENT '来源审批人实例ID（加签审批人时指向发起加签的审批人实例）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '顺签顺序',
  `status`                      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待处理 WAITING_ADD_SIGN=等待加签 APPROVED=已通过 REJECTED=已拒绝 DELEGATED=已转交 CANCELED=已取消',
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
  `action` VARCHAR(16) NOT NULL COMMENT '操作类型：SUBMIT=提交申请 APPROVE=审批通过 REJECT=审批拒绝 DELEGATE=审批转交 RECALL=发起人撤回 ADD_SIGN=发起加签 ROUTE=系统自动路由 SPLIT_TRIGGER=系统触发并行拆分 JOIN_ARRIVE=分支到达并行聚合节点 JOIN_PASS=并行聚合完成并继续流转 AUTO_APPROVE=系统自动审核通过 AUTO_REJECT=系统自动审批拒绝 TIMEOUT=节点超时自动处理触发记录 REMIND=节点超时后发送提醒',
  `node_instance_type` VARCHAR(32) DEFAULT NULL COMMENT '节点实例类型（冗余）',
  `node_instance_name` VARCHAR(100) DEFAULT NULL COMMENT '节点实例名称（冗余）',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '操作备注',
  `from_node_id` BIGINT DEFAULT NULL COMMENT '操作时所在节点ID',
  `from_node_type` VARCHAR(32) DEFAULT NULL COMMENT '来源节点实例类型（冗余）',
  `from_node_name` VARCHAR(100) DEFAULT NULL COMMENT '来源节点实例名称（冗余）',
  `to_node_id` BIGINT DEFAULT NULL COMMENT '流转到的节点ID（通过时有值）',
  `to_node_type` VARCHAR(32) DEFAULT NULL COMMENT '目标节点实例类型（冗余）',
  `to_node_name` VARCHAR(100) DEFAULT NULL COMMENT '目标节点实例名称（冗余）',
  `extra_data` JSON DEFAULT NULL COMMENT '附加数据（如附件列表、转交目标等）',
  `operated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作记录表';

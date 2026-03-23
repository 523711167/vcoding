-- 变更原因：
-- 1. 工作流节点审批人配置收敛为“一条记录一个审批主体”，不再允许 approver_value 逗号拼接多个值。
-- 2. 为 approver_type=DEPT 的审批配置新增组织展开表，保存向下展开后的当前生效组织节点。
-- 3. tb_workflow_definition 不再保留 biz_code 冗余字段。
-- 4. tb_workflow_definition 新增 workflow_json，保存前端流程设计原始 JSON。
-- 5. tb_workflow_node 不再保留 code 字段，节点前端 ID 仅用于保存时解析，不再单独落库。
-- 6. 工作流节点超时与提醒字段统一使用分钟口径。
-- 7. tb_workflow_node_approver 冗余 definition_id，便于按流程定义直接查询审批人配置。

USE `yuyu`;

ALTER TABLE `tb_workflow_node_approver`
    MODIFY COLUMN `approver_value` VARCHAR(256) NOT NULL COMMENT '审批人值：单个用户ID/角色ID/组织ID，不允许逗号拼接多个值';

SET @add_workflow_node_approver_definition_id_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tb_workflow_node_approver'
        AND COLUMN_NAME = 'definition_id'
    ),
    'SELECT 1',
    'ALTER TABLE `tb_workflow_node_approver` ADD COLUMN `definition_id` BIGINT NULL COMMENT ''所属流程定义ID'' AFTER `id`'
  )
);
PREPARE stmt_add_workflow_node_approver_definition_id FROM @add_workflow_node_approver_definition_id_sql;
EXECUTE stmt_add_workflow_node_approver_definition_id;
DEALLOCATE PREPARE stmt_add_workflow_node_approver_definition_id;

UPDATE `tb_workflow_node_approver` approver
INNER JOIN `tb_workflow_node` node
   ON node.`id` = approver.`node_id`
SET approver.`definition_id` = node.`definition_id`
WHERE approver.`definition_id` IS NULL;

ALTER TABLE `tb_workflow_node_approver`
    MODIFY COLUMN `definition_id` BIGINT NOT NULL COMMENT '所属流程定义ID';

SET @add_workflow_node_approver_definition_id_index_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tb_workflow_node_approver'
        AND INDEX_NAME = 'idx_definition_id'
    ),
    'SELECT 1',
    'ALTER TABLE `tb_workflow_node_approver` ADD INDEX `idx_definition_id` (`definition_id`)'
  )
);
PREPARE stmt_add_workflow_node_approver_definition_id_index FROM @add_workflow_node_approver_definition_id_index_sql;
EXECUTE stmt_add_workflow_node_approver_definition_id_index;
DEALLOCATE PREPARE stmt_add_workflow_node_approver_definition_id_index;

SET @drop_workflow_definition_biz_code_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tb_workflow_definition'
        AND COLUMN_NAME = 'biz_code'
    ),
    'ALTER TABLE `tb_workflow_definition` DROP COLUMN `biz_code`',
    'SELECT 1'
  )
);
PREPARE stmt_drop_workflow_definition_biz_code FROM @drop_workflow_definition_biz_code_sql;
EXECUTE stmt_drop_workflow_definition_biz_code;
DEALLOCATE PREPARE stmt_drop_workflow_definition_biz_code;

SET @add_workflow_definition_workflow_json_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tb_workflow_definition'
        AND COLUMN_NAME = 'workflow_json'
    ),
    'SELECT 1',
    'ALTER TABLE `tb_workflow_definition` ADD COLUMN `workflow_json` LONGTEXT NULL COMMENT ''前端流程设计原始JSON'' AFTER `description`'
  )
);
PREPARE stmt_add_workflow_definition_workflow_json FROM @add_workflow_definition_workflow_json_sql;
EXECUTE stmt_add_workflow_definition_workflow_json;
DEALLOCATE PREPARE stmt_add_workflow_definition_workflow_json;

SET @drop_workflow_node_code_index_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tb_workflow_node'
        AND INDEX_NAME = 'uk_definition_code'
    ),
    'ALTER TABLE `tb_workflow_node` DROP INDEX `uk_definition_code`',
    'SELECT 1'
  )
);
PREPARE stmt_drop_workflow_node_code_index FROM @drop_workflow_node_code_index_sql;
EXECUTE stmt_drop_workflow_node_code_index;
DEALLOCATE PREPARE stmt_drop_workflow_node_code_index;

SET @drop_workflow_node_code_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tb_workflow_node'
        AND COLUMN_NAME = 'code'
    ),
    'ALTER TABLE `tb_workflow_node` DROP COLUMN `code`',
    'SELECT 1'
  )
);
PREPARE stmt_drop_workflow_node_code FROM @drop_workflow_node_code_sql;
EXECUTE stmt_drop_workflow_node_code;
DEALLOCATE PREPARE stmt_drop_workflow_node_code;

SET @has_workflow_node_timeout_hours = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_workflow_node'
    AND COLUMN_NAME = 'timeout_hours'
);
SET @rename_workflow_node_timeout_minutes_sql = (
  SELECT IF(
    @has_workflow_node_timeout_hours > 0,
    'ALTER TABLE `tb_workflow_node` CHANGE COLUMN `timeout_hours` `timeout_minutes` INT DEFAULT NULL COMMENT ''超时时限（分钟）''',
    'SELECT 1'
  )
);
PREPARE stmt_rename_workflow_node_timeout_minutes FROM @rename_workflow_node_timeout_minutes_sql;
EXECUTE stmt_rename_workflow_node_timeout_minutes;
DEALLOCATE PREPARE stmt_rename_workflow_node_timeout_minutes;

SET @has_workflow_node_remind_hours = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_workflow_node'
    AND COLUMN_NAME = 'remind_hours'
);
SET @rename_workflow_node_remind_minutes_sql = (
  SELECT IF(
    @has_workflow_node_remind_hours > 0,
    'ALTER TABLE `tb_workflow_node` CHANGE COLUMN `remind_hours` `remind_minutes` INT DEFAULT NULL COMMENT ''提醒时限（分钟）''',
    'SELECT 1'
  )
);
PREPARE stmt_rename_workflow_node_remind_minutes FROM @rename_workflow_node_remind_minutes_sql;
EXECUTE stmt_rename_workflow_node_remind_minutes;
DEALLOCATE PREPARE stmt_rename_workflow_node_remind_minutes;

SET @migrate_workflow_node_timeout_minutes_sql = (
  SELECT IF(
    @has_workflow_node_timeout_hours > 0,
    'UPDATE `tb_workflow_node` SET `timeout_minutes` = `timeout_minutes` * 60 WHERE `timeout_minutes` IS NOT NULL',
    'SELECT 1'
  )
);
PREPARE stmt_migrate_workflow_node_timeout_minutes FROM @migrate_workflow_node_timeout_minutes_sql;
EXECUTE stmt_migrate_workflow_node_timeout_minutes;
DEALLOCATE PREPARE stmt_migrate_workflow_node_timeout_minutes;

SET @migrate_workflow_node_remind_minutes_sql = (
  SELECT IF(
    @has_workflow_node_remind_hours > 0,
    'UPDATE `tb_workflow_node` SET `remind_minutes` = `remind_minutes` * 60 WHERE `remind_minutes` IS NOT NULL',
    'SELECT 1'
  )
);
PREPARE stmt_migrate_workflow_node_remind_minutes FROM @migrate_workflow_node_remind_minutes_sql;
EXECUTE stmt_migrate_workflow_node_remind_minutes;
DEALLOCATE PREPARE stmt_migrate_workflow_node_remind_minutes;

CREATE TABLE IF NOT EXISTS `tb_workflow_node_approver_dept_expand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `approver_id` BIGINT NOT NULL COMMENT '来源审批人配置ID，对应 tb_workflow_node_approver.id',
  `node_id` BIGINT NOT NULL COMMENT '来源流程节点ID',
  `definition_id` BIGINT NOT NULL COMMENT '来源流程定义ID',
  `source_dept_id` BIGINT NOT NULL COMMENT '来源直接配置组织ID',
  `source_org_type` VARCHAR(32) NOT NULL COMMENT '来源组织类型冗余',
  `source_post_type` VARCHAR(64) DEFAULT NULL COMMENT '来源岗位类型冗余，非岗位来源时为空',
  `dept_id` BIGINT NOT NULL COMMENT '展开后的目标组织ID',
  `org_type` VARCHAR(32) NOT NULL COMMENT '目标组织类型冗余',
  `post_type` VARCHAR(64) DEFAULT NULL COMMENT '目标岗位类型冗余，非岗位目标时为空',
  `relation_type` VARCHAR(16) NOT NULL COMMENT '关系类型：SELF/DESCENDANT',
  `distance` INT NOT NULL DEFAULT 0 COMMENT '展开距离，自身为0，直接下级为1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approver_dept` (`approver_id`, `dept_id`),
  KEY `idx_node_id` (`node_id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_source_dept_id` (`source_dept_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点审批组织展开关系表';

DELETE FROM `tb_workflow_node_approver_dept_expand`;

INSERT INTO `tb_workflow_node_approver_dept_expand` (
  `approver_id`,
  `node_id`,
  `definition_id`,
  `source_dept_id`,
  `source_org_type`,
  `source_post_type`,
  `dept_id`,
  `org_type`,
  `post_type`,
  `relation_type`,
  `distance`
)
SELECT
  approver.`id` AS `approver_id`,
  approver.`node_id` AS `node_id`,
  node.`definition_id` AS `definition_id`,
  source_dept.`id` AS `source_dept_id`,
  source_dept.`org_type` AS `source_org_type`,
  CASE
    WHEN source_dept.`org_type` = 'POST' THEN source_dept.`post_type`
    ELSE NULL
  END AS `source_post_type`,
  target_dept.`id` AS `dept_id`,
  target_dept.`org_type` AS `org_type`,
  CASE
    WHEN target_dept.`org_type` = 'POST' THEN target_dept.`post_type`
    ELSE NULL
  END AS `post_type`,
  CASE
    WHEN target_dept.`id` = source_dept.`id` THEN 'SELF'
    ELSE 'DESCENDANT'
  END AS `relation_type`,
  target_dept.`level` - source_dept.`level` AS `distance`
FROM `tb_workflow_node_approver` approver
INNER JOIN `tb_workflow_node` node
  ON node.`id` = approver.`node_id`
 AND node.`is_deleted` = 0
INNER JOIN `tb_user_dept` source_dept
  ON source_dept.`id` = CAST(approver.`approver_value` AS UNSIGNED)
 AND source_dept.`is_deleted` = 0
 AND source_dept.`status` = 1
INNER JOIN `tb_user_dept` target_dept
  ON target_dept.`is_deleted` = 0
 AND target_dept.`status` = 1
 AND target_dept.`path` LIKE CONCAT(source_dept.`path`, '%')
WHERE approver.`is_deleted` = 0
  AND approver.`approver_type` = 'DEPT'
  AND approver.`approver_value` REGEXP '^[0-9]+$';

-- 变更原因：
-- 1. tb_workflow_transition 增加来源节点、目标节点的名称与类型冗余字段。
-- 2. 回填历史流程连线数据，便于定义层详情查询直接展示节点快照。
-- 3. tb_workflow_instance 增加当前节点名称、类型冗余字段，便于运行态直接展示当前节点快照。
-- 4. tb_workflow_node_approver_instance 将 handled_at 更名为 finished_at，并补充转交目标用户名冗余字段。

USE `yuyu`;

SET @add_workflow_transition_from_node_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_transition'
              AND COLUMN_NAME = 'from_node_name'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_transition` ADD COLUMN `from_node_name` VARCHAR(100) NULL COMMENT ''来源节点名称（冗余）'' AFTER `from_node_id`'
    )
);

PREPARE stmt_add_workflow_transition_from_node_name FROM @add_workflow_transition_from_node_name_sql;
EXECUTE stmt_add_workflow_transition_from_node_name;
DEALLOCATE PREPARE stmt_add_workflow_transition_from_node_name;

SET @add_workflow_transition_from_node_type_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_transition'
              AND COLUMN_NAME = 'from_node_type'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_transition` ADD COLUMN `from_node_type` VARCHAR(32) NULL COMMENT ''来源节点类型（冗余）'' AFTER `from_node_name`'
    )
);

PREPARE stmt_add_workflow_transition_from_node_type FROM @add_workflow_transition_from_node_type_sql;
EXECUTE stmt_add_workflow_transition_from_node_type;
DEALLOCATE PREPARE stmt_add_workflow_transition_from_node_type;

SET @add_workflow_transition_to_node_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_transition'
              AND COLUMN_NAME = 'to_node_name'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_transition` ADD COLUMN `to_node_name` VARCHAR(100) NULL COMMENT ''目标节点名称（冗余）'' AFTER `to_node_id`'
    )
);

PREPARE stmt_add_workflow_transition_to_node_name FROM @add_workflow_transition_to_node_name_sql;
EXECUTE stmt_add_workflow_transition_to_node_name;
DEALLOCATE PREPARE stmt_add_workflow_transition_to_node_name;

SET @add_workflow_transition_to_node_type_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_transition'
              AND COLUMN_NAME = 'to_node_type'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_transition` ADD COLUMN `to_node_type` VARCHAR(32) NULL COMMENT ''目标节点类型（冗余）'' AFTER `to_node_name`'
    )
);

PREPARE stmt_add_workflow_transition_to_node_type FROM @add_workflow_transition_to_node_type_sql;
EXECUTE stmt_add_workflow_transition_to_node_type;
DEALLOCATE PREPARE stmt_add_workflow_transition_to_node_type;

UPDATE `tb_workflow_transition` transition_table
LEFT JOIN `tb_workflow_node` from_node
       ON from_node.`id` = transition_table.`from_node_id`
      AND from_node.`is_deleted` = 0
LEFT JOIN `tb_workflow_node` to_node
       ON to_node.`id` = transition_table.`to_node_id`
      AND to_node.`is_deleted` = 0
SET transition_table.`from_node_name` = from_node.`name`,
    transition_table.`from_node_type` = from_node.`node_type`,
    transition_table.`to_node_name` = to_node.`name`,
    transition_table.`to_node_type` = to_node.`node_type`
WHERE transition_table.`is_deleted` = 0
  AND (
      transition_table.`from_node_name` IS NULL
      OR transition_table.`from_node_type` IS NULL
      OR transition_table.`to_node_name` IS NULL
      OR transition_table.`to_node_type` IS NULL
  );

SET @add_biz_apply_biz_definition_id_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_apply'
              AND COLUMN_NAME = 'biz_definition_id'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_biz_apply` ADD COLUMN `biz_definition_id` BIGINT NULL COMMENT ''业务定义ID'' AFTER `id`'
    )
);

PREPARE stmt_add_biz_apply_biz_definition_id FROM @add_biz_apply_biz_definition_id_sql;
EXECUTE stmt_add_biz_apply_biz_definition_id;
DEALLOCATE PREPARE stmt_add_biz_apply_biz_definition_id;

UPDATE `tb_biz_apply` apply_table
INNER JOIN `tb_biz_definition` definition_table
        ON definition_table.`biz_code` = apply_table.`biz_code`
       AND definition_table.`is_deleted` = 0
SET apply_table.`biz_definition_id` = definition_table.`id`
WHERE apply_table.`biz_definition_id` IS NULL
  AND apply_table.`biz_code` IS NOT NULL;

SET @drop_biz_apply_biz_code_index_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_apply'
              AND INDEX_NAME = 'idx_biz_code_biz_status'
        ),
        'ALTER TABLE `tb_biz_apply` DROP INDEX `idx_biz_code_biz_status`',
        'SELECT 1'
    )
);

PREPARE stmt_drop_biz_apply_biz_code_index FROM @drop_biz_apply_biz_code_index_sql;
EXECUTE stmt_drop_biz_apply_biz_code_index;
DEALLOCATE PREPARE stmt_drop_biz_apply_biz_code_index;

SET @add_biz_apply_biz_definition_status_index_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_apply'
              AND INDEX_NAME = 'idx_biz_definition_id_biz_status'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_biz_apply` ADD KEY `idx_biz_definition_id_biz_status` (`biz_definition_id`, `biz_status`)'
    )
);

PREPARE stmt_add_biz_apply_biz_definition_status_index FROM @add_biz_apply_biz_definition_status_index_sql;
EXECUTE stmt_add_biz_apply_biz_definition_status_index;
DEALLOCATE PREPARE stmt_add_biz_apply_biz_definition_status_index;

SET @drop_biz_apply_biz_code_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_apply'
              AND COLUMN_NAME = 'biz_code'
        ),
        'ALTER TABLE `tb_biz_apply` DROP COLUMN `biz_code`',
        'SELECT 1'
    )
);

PREPARE stmt_drop_biz_apply_biz_code FROM @drop_biz_apply_biz_code_sql;
EXECUTE stmt_drop_biz_apply_biz_code;
DEALLOCATE PREPARE stmt_drop_biz_apply_biz_code;

SET @add_workflow_instance_biz_id_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_instance'
              AND COLUMN_NAME = 'biz_id'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_instance` ADD COLUMN `biz_id` BIGINT NULL COMMENT ''业务定义ID'' AFTER `id`'
    )
);

PREPARE stmt_add_workflow_instance_biz_id FROM @add_workflow_instance_biz_id_sql;
EXECUTE stmt_add_workflow_instance_biz_id;
DEALLOCATE PREPARE stmt_add_workflow_instance_biz_id;

UPDATE `tb_workflow_instance` instance_table
INNER JOIN `tb_biz_apply` apply_table
        ON apply_table.`workflow_instance_id` = instance_table.`id`
       AND apply_table.`is_deleted` = 0
SET instance_table.`biz_id` = apply_table.`biz_definition_id`
WHERE instance_table.`biz_id` IS NULL
  AND apply_table.`biz_definition_id` IS NOT NULL;

SET @add_workflow_instance_biz_id_index_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_instance'
              AND INDEX_NAME = 'idx_biz_id'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_instance` ADD KEY `idx_biz_id` (`biz_id`)'
    )
);

PREPARE stmt_add_workflow_instance_biz_id_index FROM @add_workflow_instance_biz_id_index_sql;
EXECUTE stmt_add_workflow_instance_biz_id_index;
DEALLOCATE PREPARE stmt_add_workflow_instance_biz_id_index;

SET @add_workflow_instance_current_node_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_instance'
              AND COLUMN_NAME = 'current_node_name'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_instance` ADD COLUMN `current_node_name` VARCHAR(100) NULL COMMENT ''当前所在节点名称（冗余）'' AFTER `current_node_id`'
    )
);

PREPARE stmt_add_workflow_instance_current_node_name FROM @add_workflow_instance_current_node_name_sql;
EXECUTE stmt_add_workflow_instance_current_node_name;
DEALLOCATE PREPARE stmt_add_workflow_instance_current_node_name;

SET @add_workflow_instance_current_node_type_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_instance'
              AND COLUMN_NAME = 'current_node_type'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_instance` ADD COLUMN `current_node_type` VARCHAR(32) NULL COMMENT ''当前所在节点类型（冗余）'' AFTER `current_node_name`'
    )
);

PREPARE stmt_add_workflow_instance_current_node_type FROM @add_workflow_instance_current_node_type_sql;
EXECUTE stmt_add_workflow_instance_current_node_type;
DEALLOCATE PREPARE stmt_add_workflow_instance_current_node_type;

UPDATE `tb_workflow_instance` instance_table
LEFT JOIN `tb_workflow_node` node_table
       ON node_table.`id` = instance_table.`current_node_id`
      AND node_table.`is_deleted` = 0
SET instance_table.`current_node_name` = node_table.`name`,
    instance_table.`current_node_type` = node_table.`node_type`
WHERE instance_table.`is_deleted` = 0
  AND instance_table.`current_node_id` IS NOT NULL
  AND (
      instance_table.`current_node_name` IS NULL
      OR instance_table.`current_node_type` IS NULL
  );

SET @rename_workflow_node_approver_instance_handled_at_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_approver_instance'
              AND COLUMN_NAME = 'handled_at'
        ),
        'ALTER TABLE `tb_workflow_node_approver_instance` CHANGE COLUMN `handled_at` `finished_at` DATETIME NULL COMMENT ''完成时间''',
        'SELECT 1'
    )
);

PREPARE stmt_rename_workflow_node_approver_instance_handled_at FROM @rename_workflow_node_approver_instance_handled_at_sql;
EXECUTE stmt_rename_workflow_node_approver_instance_handled_at;
DEALLOCATE PREPARE stmt_rename_workflow_node_approver_instance_handled_at;

SET @add_workflow_node_approver_instance_delegate_to_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_approver_instance'
              AND COLUMN_NAME = 'delegate_to_name'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_node_approver_instance` ADD COLUMN `delegate_to_name` VARCHAR(64) NULL COMMENT ''转交目标用户姓名（冗余）'' AFTER `delegate_to`'
    )
);

PREPARE stmt_add_workflow_node_approver_instance_delegate_to_name FROM @add_workflow_node_approver_instance_delegate_to_name_sql;
EXECUTE stmt_add_workflow_node_approver_instance_delegate_to_name;
DEALLOCATE PREPARE stmt_add_workflow_node_approver_instance_delegate_to_name;

UPDATE `tb_workflow_node_approver_instance` approver_instance
LEFT JOIN `tb_user` delegate_user
       ON delegate_user.`id` = approver_instance.`delegate_to`
      AND delegate_user.`is_deleted` = 0
SET approver_instance.`delegate_to_name` = COALESCE(NULLIF(delegate_user.`real_name`, ''), delegate_user.`username`)
WHERE approver_instance.`delegate_to` IS NOT NULL
  AND approver_instance.`delegate_to_name` IS NULL;

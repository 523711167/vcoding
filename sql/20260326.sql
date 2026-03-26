-- 变更原因：
-- 1. tb_workflow_node_instance 的 node_id/node_name/node_type 实际表达的是节点定义快照，
--    现统一改名为 definition_node_id/definition_node_name/definition_node_type。
-- 2. tb_workflow_node_instance 新增 parallel_branch_root_id，用于冗余当前节点所属并行分支的根拆分节点实例ID。

USE `yuyu`;

SET @rename_workflow_node_instance_node_id_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'node_id'
        ),
        'ALTER TABLE `tb_workflow_node_instance` CHANGE COLUMN `node_id` `definition_node_id` BIGINT NOT NULL COMMENT ''节点定义ID''',
        'SELECT 1'
    )
);

PREPARE stmt_rename_workflow_node_instance_node_id FROM @rename_workflow_node_instance_node_id_sql;
EXECUTE stmt_rename_workflow_node_instance_node_id;
DEALLOCATE PREPARE stmt_rename_workflow_node_instance_node_id;

SET @modify_workflow_node_instance_definition_node_id_comment_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'definition_node_id'
        ),
        'ALTER TABLE `tb_workflow_node_instance` MODIFY COLUMN `definition_node_id` BIGINT NOT NULL COMMENT ''节点定义ID''',
        'SELECT 1'
    )
);

PREPARE stmt_modify_workflow_node_instance_definition_node_id_comment FROM @modify_workflow_node_instance_definition_node_id_comment_sql;
EXECUTE stmt_modify_workflow_node_instance_definition_node_id_comment;
DEALLOCATE PREPARE stmt_modify_workflow_node_instance_definition_node_id_comment;

SET @rename_workflow_node_instance_node_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'node_name'
        ),
        'ALTER TABLE `tb_workflow_node_instance` CHANGE COLUMN `node_name` `definition_node_name` VARCHAR(100) NOT NULL COMMENT ''节点定义名称（冗余）''',
        'SELECT 1'
    )
);

PREPARE stmt_rename_workflow_node_instance_node_name FROM @rename_workflow_node_instance_node_name_sql;
EXECUTE stmt_rename_workflow_node_instance_node_name;
DEALLOCATE PREPARE stmt_rename_workflow_node_instance_node_name;

SET @modify_workflow_node_instance_definition_node_name_comment_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'definition_node_name'
        ),
        'ALTER TABLE `tb_workflow_node_instance` MODIFY COLUMN `definition_node_name` VARCHAR(100) NOT NULL COMMENT ''节点定义名称（冗余）''',
        'SELECT 1'
    )
);

PREPARE stmt_modify_workflow_node_instance_definition_node_name_comment FROM @modify_workflow_node_instance_definition_node_name_comment_sql;
EXECUTE stmt_modify_workflow_node_instance_definition_node_name_comment;
DEALLOCATE PREPARE stmt_modify_workflow_node_instance_definition_node_name_comment;

SET @rename_workflow_node_instance_node_type_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'node_type'
        ),
        'ALTER TABLE `tb_workflow_node_instance` CHANGE COLUMN `node_type` `definition_node_type` VARCHAR(32) NOT NULL COMMENT ''节点定义类型（冗余）''',
        'SELECT 1'
    )
);

PREPARE stmt_rename_workflow_node_instance_node_type FROM @rename_workflow_node_instance_node_type_sql;
EXECUTE stmt_rename_workflow_node_instance_node_type;
DEALLOCATE PREPARE stmt_rename_workflow_node_instance_node_type;

SET @modify_workflow_node_instance_definition_node_type_comment_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'definition_node_type'
        ),
        'ALTER TABLE `tb_workflow_node_instance` MODIFY COLUMN `definition_node_type` VARCHAR(32) NOT NULL COMMENT ''节点定义类型（冗余）''',
        'SELECT 1'
    )
);

PREPARE stmt_modify_workflow_node_instance_definition_node_type_comment FROM @modify_workflow_node_instance_definition_node_type_comment_sql;
EXECUTE stmt_modify_workflow_node_instance_definition_node_type_comment;
DEALLOCATE PREPARE stmt_modify_workflow_node_instance_definition_node_type_comment;

SET @add_workflow_node_instance_parallel_branch_root_id_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'parallel_branch_root_id'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_node_instance` ADD COLUMN `parallel_branch_root_id` BIGINT NULL COMMENT ''所属并行分支根拆分节点实例ID'' AFTER `definition_node_type`'
    )
);

PREPARE stmt_add_workflow_node_instance_parallel_branch_root_id FROM @add_workflow_node_instance_parallel_branch_root_id_sql;
EXECUTE stmt_add_workflow_node_instance_parallel_branch_root_id;
DEALLOCATE PREPARE stmt_add_workflow_node_instance_parallel_branch_root_id;

-- 变更原因：
-- 1. tb_workflow_node_instance 的 node_id/node_name/node_type 实际表达的是节点定义快照，
--    原注释容易让人误解为节点实例自身字段，现统一调整为“节点定义”语义。

USE `yuyu`;

SET @modify_workflow_node_instance_node_id_comment_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'node_id'
        ),
        'ALTER TABLE `tb_workflow_node_instance` MODIFY COLUMN `node_id` BIGINT NOT NULL COMMENT ''节点定义ID''',
        'SELECT 1'
    )
);

PREPARE stmt_modify_workflow_node_instance_node_id_comment FROM @modify_workflow_node_instance_node_id_comment_sql;
EXECUTE stmt_modify_workflow_node_instance_node_id_comment;
DEALLOCATE PREPARE stmt_modify_workflow_node_instance_node_id_comment;

SET @modify_workflow_node_instance_node_name_comment_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'node_name'
        ),
        'ALTER TABLE `tb_workflow_node_instance` MODIFY COLUMN `node_name` VARCHAR(100) NOT NULL COMMENT ''节点定义名称（冗余）''',
        'SELECT 1'
    )
);

PREPARE stmt_modify_workflow_node_instance_node_name_comment FROM @modify_workflow_node_instance_node_name_comment_sql;
EXECUTE stmt_modify_workflow_node_instance_node_name_comment;
DEALLOCATE PREPARE stmt_modify_workflow_node_instance_node_name_comment;

SET @modify_workflow_node_instance_node_type_comment_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'node_type'
        ),
        'ALTER TABLE `tb_workflow_node_instance` MODIFY COLUMN `node_type` VARCHAR(32) NOT NULL COMMENT ''节点定义类型（冗余）''',
        'SELECT 1'
    )
);

PREPARE stmt_modify_workflow_node_instance_node_type_comment FROM @modify_workflow_node_instance_node_type_comment_sql;
EXECUTE stmt_modify_workflow_node_instance_node_type_comment;
DEALLOCATE PREPARE stmt_modify_workflow_node_instance_node_type_comment;

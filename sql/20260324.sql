-- 变更原因：
-- 1. 工作流连线增加显式默认分支字段 is_default。
-- 2. 支持 CONDITION / PARALLEL_SPLIT 多分支时按默认分支规则做发布与保存校验。

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

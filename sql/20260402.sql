-- 变更原因：
-- 1. 新增 tb_login_log，记录用户名密码登录成功/失败留痕，满足登录审计追踪要求。

CREATE TABLE IF NOT EXISTS `tb_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '登录用户ID，失败场景为空',
  `username` VARCHAR(64) DEFAULT NULL COMMENT '登录用户名',
  `result` VARCHAR(16) NOT NULL COMMENT '登录结果：SUCCESS/FAIL',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP地址',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '客户端User-Agent',
  `login_at` DATETIME NOT NULL COMMENT '登录发生时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_result` (`result`),
  KEY `idx_login_at` (`login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';

-- 2. 业务定义绑定关系由流程定义ID切换为流程定义编码，避免流程升版后绑定失效。

USE `yuyu`;

SET @add_biz_definition_workflow_code_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_definition'
              AND COLUMN_NAME = 'workflow_definition_code'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_biz_definition` ADD COLUMN `workflow_definition_code` VARCHAR(64) NULL COMMENT ''绑定的流程定义编码'' AFTER `biz_desc`'
    )
);

PREPARE stmt_add_biz_definition_workflow_code FROM @add_biz_definition_workflow_code_sql;
EXECUTE stmt_add_biz_definition_workflow_code;
DEALLOCATE PREPARE stmt_add_biz_definition_workflow_code;

SET @fill_biz_definition_workflow_code_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_definition'
              AND COLUMN_NAME = 'workflow_definition_code'
        ) AND EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_definition'
              AND COLUMN_NAME = 'workflow_definition_id'
        ),
        'UPDATE `tb_biz_definition` b LEFT JOIN `tb_workflow_definition` w ON w.id = b.workflow_definition_id SET b.workflow_definition_code = w.code WHERE (b.workflow_definition_code IS NULL OR b.workflow_definition_code = '''')',
        'SELECT 1'
    )
);

PREPARE stmt_fill_biz_definition_workflow_code FROM @fill_biz_definition_workflow_code_sql;
EXECUTE stmt_fill_biz_definition_workflow_code;
DEALLOCATE PREPARE stmt_fill_biz_definition_workflow_code;

SET @drop_biz_definition_workflow_id_index_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_definition'
              AND INDEX_NAME = 'idx_workflow_definition_id'
        ),
        'ALTER TABLE `tb_biz_definition` DROP INDEX `idx_workflow_definition_id`',
        'SELECT 1'
    )
);

PREPARE stmt_drop_biz_definition_workflow_id_index FROM @drop_biz_definition_workflow_id_index_sql;
EXECUTE stmt_drop_biz_definition_workflow_id_index;
DEALLOCATE PREPARE stmt_drop_biz_definition_workflow_id_index;

SET @drop_biz_definition_workflow_id_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_definition'
              AND COLUMN_NAME = 'workflow_definition_id'
        ),
        'ALTER TABLE `tb_biz_definition` DROP COLUMN `workflow_definition_id`',
        'SELECT 1'
    )
);

PREPARE stmt_drop_biz_definition_workflow_id FROM @drop_biz_definition_workflow_id_sql;
EXECUTE stmt_drop_biz_definition_workflow_id;
DEALLOCATE PREPARE stmt_drop_biz_definition_workflow_id;

SET @modify_biz_definition_workflow_code_not_null_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_definition'
              AND COLUMN_NAME = 'workflow_definition_code'
              AND IS_NULLABLE = 'YES'
        ) AND NOT EXISTS(
            SELECT 1
            FROM tb_biz_definition
            WHERE workflow_definition_code IS NULL OR workflow_definition_code = ''
        ),
        'ALTER TABLE `tb_biz_definition` MODIFY COLUMN `workflow_definition_code` VARCHAR(64) NOT NULL COMMENT ''绑定的流程定义编码''',
        'SELECT 1'
    )
);

PREPARE stmt_modify_biz_definition_workflow_code_not_null FROM @modify_biz_definition_workflow_code_not_null_sql;
EXECUTE stmt_modify_biz_definition_workflow_code_not_null;
DEALLOCATE PREPARE stmt_modify_biz_definition_workflow_code_not_null;

SET @add_biz_definition_workflow_code_index_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_biz_definition'
              AND INDEX_NAME = 'idx_workflow_definition_code'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_biz_definition` ADD INDEX `idx_workflow_definition_code` (`workflow_definition_code`)'
    )
);

PREPARE stmt_add_biz_definition_workflow_code_index FROM @add_biz_definition_workflow_code_index_sql;
EXECUTE stmt_add_biz_definition_workflow_code_index;
DEALLOCATE PREPARE stmt_add_biz_definition_workflow_code_index;

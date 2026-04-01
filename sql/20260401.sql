-- 变更原因：
-- 1. 新增 tb_workflow_parallel_scope，用于表达运行时并行作用域，支撑嵌套并行拆分/聚合。
-- 2. tb_workflow_node_instance 新增 parallel_scope_id，节点实例运行态统一归属到具体并行作用域实例。
-- 3. parallel_branch_root_id 暂时保留兼容，后续业务逻辑逐步切换到 parallel_scope_id。

CREATE TABLE IF NOT EXISTS `tb_workflow_parallel_scope` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
  `definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `split_definition_node_id` BIGINT NOT NULL COMMENT '并行拆分节点定义ID',
  `split_definition_node_name` VARCHAR(100) NOT NULL COMMENT '并行拆分节点定义名称（冗余）',
  `split_definition_node_type` VARCHAR(32) NOT NULL COMMENT '并行拆分节点定义类型（冗余）',
  `join_definition_node_id` BIGINT NOT NULL COMMENT '并行聚合节点定义ID',
  `join_definition_node_name` VARCHAR(100) NOT NULL COMMENT '并行聚合节点定义名称（冗余）',
  `join_definition_node_type` VARCHAR(32) NOT NULL COMMENT '并行聚合节点定义类型（冗余）',
  `parent_scope_id` BIGINT DEFAULT NULL COMMENT '父并行作用域ID，最外层为空',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '作用域状态：ACTIVE=进行中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已取消',
  `expected_branch_count` INT NOT NULL DEFAULT 0 COMMENT '理论应汇聚分支数',
  `arrived_branch_count` INT NOT NULL DEFAULT 0 COMMENT '当前已到达聚合节点的分支数',
  `finished_at` DATETIME DEFAULT NULL COMMENT '作用域完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_parent_scope_id` (`parent_scope_id`),
  KEY `idx_split_definition_node_id` (`split_definition_node_id`),
  KEY `idx_join_definition_node_id` (`join_definition_node_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程并行作用域表';

SET @ddl_sql = (
    SELECT IF (
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND COLUMN_NAME = 'parallel_scope_id'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_node_instance` ADD COLUMN `parallel_scope_id` BIGINT NULL COMMENT ''所属并行作用域ID'' AFTER `parallel_branch_root_id`'
    )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = (
    SELECT IF (
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node_instance'
              AND INDEX_NAME = 'idx_parallel_scope_id'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_node_instance` ADD KEY `idx_parallel_scope_id` (`parallel_scope_id`)'
    )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

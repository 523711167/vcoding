-- 变更原因：
-- 1. tb_workflow_node 新增 parallel_split_node_id，用于冗余当前节点所属的最近一层并行拆分节点定义ID。
-- 2. 便于并行分支、并行聚合和嵌套并行场景下直接判断节点所属并行段。

SET @ddl_sql = (
    SELECT IF (
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tb_workflow_node'
              AND COLUMN_NAME = 'parallel_split_node_id'
        ),
        'SELECT 1',
        'ALTER TABLE `tb_workflow_node` ADD COLUMN `parallel_split_node_id` BIGINT NULL COMMENT ''所属最近一层并行拆分节点定义ID'' AFTER `node_type`'
    )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

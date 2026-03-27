-- 变更原因：
-- 1. tb_workflow_node_approver.approver_value 统一收敛为 BIGINT，避免继续用字符串承载用户/角色/组织主键。
-- 2. 对应后端实体、入参出参和审批解析逻辑统一改为 Long 口径，减少运行时转换与字符串比较。

USE `yuyu`;

ALTER TABLE `tb_workflow_node_approver`
    MODIFY COLUMN `approver_value` BIGINT NOT NULL COMMENT '审批人值：单个用户ID/角色ID/组织ID';

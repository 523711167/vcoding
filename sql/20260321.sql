-- 变更原因：
-- 1. 将角色表 tb_user_role.data_scope 从数值型字段调整为字符串字段，统一按语义编码存储。
-- 2. 便于数据库数据直观表达、接口参数统一和设计文档口径统一。

USE `yuyu`;

ALTER TABLE `tb_user_role`
  MODIFY COLUMN `data_scope` VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '数据权限范围：ALL/CUSTOM_DEPT/CURRENT_AND_CHILD_DEPT/CURRENT_DEPT/SELF';

UPDATE `tb_user_role`
SET `data_scope` = CASE `data_scope`
    WHEN '1' THEN 'ALL'
    WHEN '2' THEN 'CUSTOM_DEPT'
    WHEN '3' THEN 'CURRENT_AND_CHILD_DEPT'
    WHEN '4' THEN 'CURRENT_DEPT'
    WHEN '5' THEN 'SELF'
    ELSE `data_scope`
END;

-- 变更原因：
-- 1. 将角色表 tb_user_role.data_scope 从数值型字段调整为字符串字段，统一按语义编码存储。
-- 2. 新增角色数据权限 direct / expand 分表设计，支持直接授权与展开生效范围分离。
-- 3. 为 tb_user_role_dept 增加组织类型与岗位类型冗余字段，并新增 tb_user_role_dept_expand。

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

ALTER TABLE `tb_user_role_dept`
  ADD COLUMN `org_type` VARCHAR(16) NULL COMMENT '组织类型冗余：GROUP/COMPANY/DEPT/POST' AFTER `dept_id`,
  ADD COLUMN `post_type` VARCHAR(64) NULL COMMENT '岗位类型冗余；仅当 org_type=POST 时有值' AFTER `org_type`;

UPDATE `tb_user_role_dept` rel
JOIN `tb_user_dept` dept ON dept.`id` = rel.`dept_id`
SET rel.`org_type` = dept.`org_type`,
    rel.`post_type` = CASE
        WHEN dept.`org_type` = 'POST' THEN dept.`post_type`
        ELSE NULL
    END;

ALTER TABLE `tb_user_role_dept`
  MODIFY COLUMN `org_type` VARCHAR(16) NOT NULL COMMENT '组织类型冗余：GROUP/COMPANY/DEPT/POST';

CREATE TABLE IF NOT EXISTS `tb_user_role_dept_expand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID（关联tb_user_role.id）',
  `source_rel_id` BIGINT NOT NULL COMMENT '来源直接绑定关系ID（关联tb_user_role_dept.id）',
  `source_dept_id` BIGINT NOT NULL COMMENT '来源直接绑定组织ID（关联tb_user_dept.id）',
  `source_org_type` VARCHAR(16) NOT NULL COMMENT '来源组织类型冗余：GROUP/COMPANY/DEPT/POST',
  `source_post_type` VARCHAR(64) DEFAULT NULL COMMENT '来源岗位类型冗余；非岗位来源为空',
  `dept_id` BIGINT NOT NULL COMMENT '展开后的目标组织ID（关联tb_user_dept.id）',
  `org_type` VARCHAR(16) NOT NULL COMMENT '目标组织类型冗余：GROUP/COMPANY/DEPT/POST',
  `post_type` VARCHAR(64) DEFAULT NULL COMMENT '目标岗位类型冗余；非岗位目标为空',
  `relation_type` VARCHAR(16) NOT NULL COMMENT '关系类型：SELF/DESCENDANT',
  `distance` INT NOT NULL DEFAULT 0 COMMENT '展开距离：自身=0，直接下级=1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_source_dept` (`role_id`, `source_rel_id`, `dept_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_source_dept_id` (`source_dept_id`),
  KEY `idx_role_dept_id` (`role_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-数据权限组织展开生效关系表';

DELETE FROM `tb_user_role_dept_expand`;

INSERT INTO `tb_user_role_dept_expand` (
  `role_id`,
  `source_rel_id`,
  `source_dept_id`,
  `source_org_type`,
  `source_post_type`,
  `dept_id`,
  `org_type`,
  `post_type`,
  `relation_type`,
  `distance`
)
WITH RECURSIVE `role_dept_expand` AS (
  SELECT
    rel.`role_id`,
    rel.`id` AS `source_rel_id`,
    rel.`dept_id` AS `source_dept_id`,
    rel.`org_type` AS `source_org_type`,
    rel.`post_type` AS `source_post_type`,
    dept.`id` AS `dept_id`,
    dept.`org_type`,
    CASE
      WHEN dept.`org_type` = 'POST' THEN dept.`post_type`
      ELSE NULL
    END AS `post_type`,
    CAST('SELF' AS CHAR(16)) AS `relation_type`,
    0 AS `distance`
  FROM `tb_user_role_dept` rel
  JOIN `tb_user_dept` dept ON dept.`id` = rel.`dept_id`
  JOIN `tb_user_role` role ON role.`id` = rel.`role_id`
  WHERE role.`data_scope` = 'CUSTOM_DEPT'
    AND dept.`status` = 1
    AND dept.`is_deleted` = 0

  UNION ALL

  SELECT
    exp.`role_id`,
    exp.`source_rel_id`,
    exp.`source_dept_id`,
    exp.`source_org_type`,
    exp.`source_post_type`,
    child.`id` AS `dept_id`,
    child.`org_type`,
    CASE
      WHEN child.`org_type` = 'POST' THEN child.`post_type`
      ELSE NULL
    END AS `post_type`,
    CAST('DESCENDANT' AS CHAR(16)) AS `relation_type`,
    exp.`distance` + 1 AS `distance`
  FROM `role_dept_expand` exp
  JOIN `tb_user_dept` child ON child.`parent_id` = exp.`dept_id`
  WHERE child.`status` = 1
    AND child.`is_deleted` = 0
)
SELECT
  `role_id`,
  `source_rel_id`,
  `source_dept_id`,
  `source_org_type`,
  `source_post_type`,
  `dept_id`,
  `org_type`,
  `post_type`,
  `relation_type`,
  `distance`
FROM `role_dept_expand`;

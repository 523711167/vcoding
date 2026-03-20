-- 变更原因：
-- 1. 用户组织直接绑定关系与展开生效关系需要分表存储，避免 direct / expand 语义冲突。
-- 2. 需要支持按组织查询当前生效用户、按来源追溯展开关系，以及组织树变更后的受影响用户重建。

USE `yuyu`;

CREATE TABLE IF NOT EXISTS `tb_user_dept_rel_expand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID（关联tb_user.id）',
  `source_rel_id` BIGINT NOT NULL COMMENT '来源直接绑定关系ID（关联tb_user_dept_rel.id）',
  `source_dept_id` BIGINT NOT NULL COMMENT '来源直接绑定组织ID（关联tb_user_dept.id）',
  `source_org_type` VARCHAR(16) NOT NULL COMMENT '来源组织类型冗余：GROUP/COMPANY/DEPT/POST',
  `source_post_type` VARCHAR(64) DEFAULT NULL COMMENT '来源岗位类型冗余；非岗位来源为空',
  `source_is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT '是否来源于主组织绑定：1=是 0=否',
  `dept_id` BIGINT NOT NULL COMMENT '展开后的目标组织ID（关联tb_user_dept.id）',
  `org_type` VARCHAR(16) NOT NULL COMMENT '目标组织类型冗余：GROUP/COMPANY/DEPT/POST',
  `post_type` VARCHAR(64) DEFAULT NULL COMMENT '目标岗位类型冗余；非岗位目标为空',
  `relation_type` VARCHAR(16) NOT NULL COMMENT '关系类型：SELF/DESCENDANT',
  `distance` INT NOT NULL DEFAULT 0 COMMENT '展开距离：自身=0，直接下级=1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_source_dept` (`user_id`, `source_rel_id`, `dept_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_source_dept_id` (`source_dept_id`),
  KEY `idx_user_dept_id` (`user_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-组织展开生效关系表';

DELETE FROM `tb_user_dept_rel_expand`;

INSERT INTO `tb_user_dept_rel_expand` (
  `user_id`,
  `source_rel_id`,
  `source_dept_id`,
  `source_org_type`,
  `source_post_type`,
  `source_is_primary`,
  `dept_id`,
  `org_type`,
  `post_type`,
  `relation_type`,
  `distance`
)
WITH RECURSIVE `dept_expand` AS (
  SELECT
    rel.`user_id`,
    rel.`id` AS `source_rel_id`,
    rel.`dept_id` AS `source_dept_id`,
    COALESCE(rel.`org_type`, dept.`org_type`) AS `source_org_type`,
    CASE
      WHEN COALESCE(rel.`org_type`, dept.`org_type`) = 'POST' THEN COALESCE(rel.`post_type`, dept.`post_type`)
      ELSE NULL
    END AS `source_post_type`,
    rel.`is_primary` AS `source_is_primary`,
    dept.`id` AS `dept_id`,
    dept.`org_type`,
    CASE
      WHEN dept.`org_type` = 'POST' THEN dept.`post_type`
      ELSE NULL
    END AS `post_type`,
    CAST('SELF' AS CHAR(16)) AS `relation_type`,
    0 AS `distance`
  FROM `tb_user_dept_rel` rel
  JOIN `tb_user_dept` dept ON dept.`id` = rel.`dept_id`
  WHERE dept.`status` = 1
    AND dept.`is_deleted` = 0

  UNION ALL

  SELECT
    exp.`user_id`,
    exp.`source_rel_id`,
    exp.`source_dept_id`,
    exp.`source_org_type`,
    exp.`source_post_type`,
    exp.`source_is_primary`,
    child.`id` AS `dept_id`,
    child.`org_type`,
    CASE
      WHEN child.`org_type` = 'POST' THEN child.`post_type`
      ELSE NULL
    END AS `post_type`,
    CAST('DESCENDANT' AS CHAR(16)) AS `relation_type`,
    exp.`distance` + 1 AS `distance`
  FROM `dept_expand` exp
  JOIN `tb_user_dept` child ON child.`parent_id` = exp.`dept_id`
  WHERE child.`status` = 1
    AND child.`is_deleted` = 0
)
SELECT
  `user_id`,
  `source_rel_id`,
  `source_dept_id`,
  `source_org_type`,
  `source_post_type`,
  `source_is_primary`,
  `dept_id`,
  `org_type`,
  `post_type`,
  `relation_type`,
  `distance`
FROM `dept_expand`;

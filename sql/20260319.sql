-- 变更原因：
-- 1. 组织模型新增集团、公司、部门、岗位四级类型约束，需要在组织表记录组织类型与岗位类型。
-- 2. 用户-组织关联需要冗余组织类型与岗位类型，便于用户组织归属查询、岗位归属查询与后续权限解析。

USE `yuyu`;

ALTER TABLE `tb_user_dept`
  ADD COLUMN `org_type` VARCHAR(16) NOT NULL DEFAULT 'DEPT' COMMENT '组织类型：GROUP/COMPANY/DEPT/POST' AFTER `code`,
  ADD COLUMN `post_type` VARCHAR(64) DEFAULT NULL COMMENT '岗位类型，org_type=POST时必填' AFTER `org_type`;

ALTER TABLE `tb_user_dept_rel`
  ADD COLUMN `org_type` VARCHAR(16) DEFAULT NULL COMMENT '组织类型，冗余自tb_user_dept.org_type' AFTER `dept_id`,
  ADD COLUMN `post_type` VARCHAR(64) DEFAULT NULL COMMENT '岗位类型，冗余自tb_user_dept.post_type' AFTER `org_type`;

UPDATE `tb_user_dept`
SET `org_type` = CASE
    WHEN `parent_id` = 0 THEN 'GROUP'
    ELSE 'DEPT'
END,
    `post_type` = NULL
WHERE `is_deleted` = 0;

UPDATE `tb_user_dept_rel` rel
JOIN `tb_user_dept` dept ON dept.`id` = rel.`dept_id`
SET rel.`org_type` = dept.`org_type`,
    rel.`post_type` = CASE
        WHEN dept.`org_type` = 'POST' THEN dept.`post_type`
        ELSE NULL
    END;

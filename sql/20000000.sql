-- 变更原因：
-- 1. 该文件调整为 `20000000.sql`，用于明确标识初始化基线 SQL。
-- 2. 本文件承载用户、角色、组织、菜单及关联关系的初始化建表与初始数据。

USE `yuyu`;

CREATE TABLE IF NOT EXISTS `tb_user_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，顶级部门为0',
  `name` VARCHAR(100) NOT NULL COMMENT '部门名称',
  `code` VARCHAR(64) DEFAULT NULL COMMENT '部门编码（同级唯一）',
  `path` VARCHAR(500) NOT NULL COMMENT '祖级路径，格式：/1/5/12/',
  `level` INT NOT NULL DEFAULT 1 COMMENT '层级深度，顶级为1',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '同级排序值，升序展示',
  `leader_id` BIGINT DEFAULT NULL COMMENT '部门主管用户ID',
  `leader_name` VARCHAR(64) DEFAULT NULL COMMENT '部门主管姓名（冗余）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parent_code_deleted` (`parent_id`, `code`, `is_deleted`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_path` (`path`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE IF NOT EXISTS `tb_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(64) NOT NULL COMMENT '角色编码（全局唯一）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=停用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，升序展示',
  `data_scope` VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '数据权限范围：ALL/CUSTOM_DEPT/CURRENT_AND_CHILD_DEPT/CURRENT_DEPT/SELF',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `tb_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录用户名（全局唯一）',
  `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt）',
  `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(256) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=停用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `tb_user_role_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tb_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

CREATE TABLE IF NOT EXISTS `tb_user_dept_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门ID',
  `is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT '是否主部门：1=是 0=否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tb_user_dept` (`user_id`, `dept_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-部门关联表';

CREATE TABLE IF NOT EXISTS `tb_user_role_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_dept` (`role_id`, `dept_id`),
  KEY `idx_role_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-自定义数据权限部门表';

CREATE TABLE IF NOT EXISTS `tb_sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父节点ID，顶级节点为0',
  `type` TINYINT NOT NULL COMMENT '类型：1=目录 2=菜单 3=按钮',
  `name` VARCHAR(64) NOT NULL COMMENT '菜单/按钮名称',
  `permission` VARCHAR(128) DEFAULT NULL COMMENT '权限标识，按钮类型必填',
  `path` VARCHAR(256) DEFAULT NULL COMMENT '前端路由地址',
  `component` VARCHAR(256) DEFAULT NULL COMMENT '前端组件路径',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标标识',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '同级排序值，升序展示',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示在导航菜单中：1=显示 0=隐藏',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission` (`permission`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/按钮表';

CREATE TABLE IF NOT EXISTS `tb_user_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单/按钮ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';

INSERT INTO `tb_sys_menu`
(`id`, `parent_id`, `type`, `name`, `permission`, `path`, `component`, `icon`, `sort_order`, `visible`, `status`)
VALUES
(1000, 0, 1, '系统管理', NULL, '/system', NULL, 'setting', 10, 1, 1),
(1100, 1000, 2, '用户管理', 'sys:user:list', '/system/user', 'system/user/index', 'user', 10, 1, 1),
(1101, 1100, 3, '新增用户', 'sys:user:add', NULL, NULL, NULL, 10, 1, 1),
(1102, 1100, 3, '修改用户', 'sys:user:edit', NULL, NULL, NULL, 20, 1, 1),
(1103, 1100, 3, '删除用户', 'sys:user:delete', NULL, NULL, NULL, 30, 1, 1),
(1104, 1100, 3, '重置密码', 'sys:user:reset-pwd', NULL, NULL, NULL, 40, 1, 1),
(1105, 1100, 3, '分配角色', 'sys:user:assign-role', NULL, NULL, NULL, 50, 1, 1),
(1106, 1100, 3, '分配组织', 'sys:user:assign-dept', NULL, NULL, NULL, 60, 1, 1),
(1200, 1000, 2, '角色管理', 'sys:role:list', '/system/role', 'system/role/index', 'peoples', 20, 1, 1),
(1201, 1200, 3, '新增角色', 'sys:role:add', NULL, NULL, NULL, 10, 1, 1),
(1202, 1200, 3, '修改角色', 'sys:role:edit', NULL, NULL, NULL, 20, 1, 1),
(1203, 1200, 3, '删除角色', 'sys:role:delete', NULL, NULL, NULL, 30, 1, 1),
(1204, 1200, 3, '分配菜单', 'sys:role:assign-menu', NULL, NULL, NULL, 40, 1, 1),
(1205, 1200, 3, '数据权限', 'sys:role:data-scope', NULL, NULL, NULL, 50, 1, 1),
(1300, 1000, 2, '组织管理', 'sys:dept:tree', '/system/dept', 'system/dept/index', 'tree', 30, 1, 1),
(1301, 1300, 3, '新增组织', 'sys:dept:add', NULL, NULL, NULL, 10, 1, 1),
(1302, 1300, 3, '修改组织', 'sys:dept:edit', NULL, NULL, NULL, 20, 1, 1),
(1303, 1300, 3, '删除组织', 'sys:dept:delete', NULL, NULL, NULL, 30, 1, 1),
(1304, 1300, 3, '移动组织', 'sys:dept:move', NULL, NULL, NULL, 40, 1, 1),
(1400, 1000, 2, '菜单管理', 'sys:menu:tree', '/system/menu', 'system/menu/index', 'menu', 40, 1, 1),
(1401, 1400, 3, '新增菜单', 'sys:menu:add', NULL, NULL, NULL, 10, 1, 1),
(1402, 1400, 3, '修改菜单', 'sys:menu:edit', NULL, NULL, NULL, 20, 1, 1),
(1403, 1400, 3, '删除菜单', 'sys:menu:delete', NULL, NULL, NULL, 30, 1, 1)
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`type` = VALUES(`type`),
`name` = VALUES(`name`),
`permission` = VALUES(`permission`),
`path` = VALUES(`path`),
`component` = VALUES(`component`),
`icon` = VALUES(`icon`),
`sort_order` = VALUES(`sort_order`),
`visible` = VALUES(`visible`),
`status` = VALUES(`status`);

INSERT INTO `tb_user_dept`
(`id`, `parent_id`, `name`, `code`, `path`, `level`, `sort_order`, `leader_id`, `leader_name`, `status`)
VALUES
(1, 0, '总部', 'HEAD_OFFICE', '/1/', 1, 10, 1, '系统管理员', 1)
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`name` = VALUES(`name`),
`code` = VALUES(`code`),
`path` = VALUES(`path`),
`level` = VALUES(`level`),
`sort_order` = VALUES(`sort_order`),
`leader_id` = VALUES(`leader_id`),
`leader_name` = VALUES(`leader_name`),
`status` = VALUES(`status`);

INSERT INTO `tb_user_role`
(`id`, `name`, `code`, `description`, `status`, `sort_order`, `data_scope`)
VALUES
(1, '系统管理员', 'ADMIN', '拥有系统全部管理权限', 1, 10, 'ALL')
ON DUPLICATE KEY UPDATE
`name` = VALUES(`name`),
`code` = VALUES(`code`),
`description` = VALUES(`description`),
`status` = VALUES(`status`),
`sort_order` = VALUES(`sort_order`),
`data_scope` = VALUES(`data_scope`);

INSERT INTO `tb_user`
(`id`, `username`, `password`, `real_name`, `email`, `mobile`, `avatar`, `status`, `last_login_at`)
VALUES
(1, 'admin', '$2a$10$Hf3Pqi4aXMTReDLGi/W9a.0I2ohxTWUMWxbbwgdxcWmVe1vF4HbPO', '系统管理员', 'admin@yuyu.com', '13800000000', NULL, 1, NULL)
ON DUPLICATE KEY UPDATE
`username` = VALUES(`username`),
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`email` = VALUES(`email`),
`mobile` = VALUES(`mobile`),
`avatar` = VALUES(`avatar`),
`status` = VALUES(`status`);

INSERT INTO `tb_user_role_rel`
(`id`, `user_id`, `role_id`)
VALUES
(1, 1, 1)
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`role_id` = VALUES(`role_id`);

INSERT INTO `tb_user_dept_rel`
(`id`, `user_id`, `dept_id`, `is_primary`)
VALUES
(1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`dept_id` = VALUES(`dept_id`),
`is_primary` = VALUES(`is_primary`);

INSERT INTO `tb_user_role_menu`
(`id`, `role_id`, `menu_id`)
VALUES
(1, 1, 1000),
(2, 1, 1100),
(3, 1, 1101),
(4, 1, 1102),
(5, 1, 1103),
(6, 1, 1104),
(7, 1, 1105),
(8, 1, 1106),
(9, 1, 1200),
(10, 1, 1201),
(11, 1, 1202),
(12, 1, 1203),
(13, 1, 1204),
(14, 1, 1205),
(15, 1, 1300),
(16, 1, 1301),
(17, 1, 1302),
(18, 1, 1303),
(19, 1, 1304),
(20, 1, 1400),
(21, 1, 1401),
(22, 1, 1402),
(23, 1, 1403)
ON DUPLICATE KEY UPDATE
`role_id` = VALUES(`role_id`),
`menu_id` = VALUES(`menu_id`);

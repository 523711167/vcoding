-- 变更原因：新增发起人取消流程能力，保留原撤回语义不变，补充动作与终态注释，为业务申请增加取消原因字段，并同步最新菜单初始化数据。

ALTER TABLE `tb_biz_apply`
    ADD COLUMN `cancel_reason` VARCHAR(500) NULL COMMENT '发起人取消原因，仅发起人取消流程时有值' AFTER `workflow_instance_id`;

ALTER TABLE `tb_biz_apply`
    MODIFY COLUMN `biz_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '业务申请状态：DRAFT=草稿 PENDING=审批中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回 INITIATOR_CANCELED=已取消';

ALTER TABLE `tb_workflow_instance`
    MODIFY COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'RUNNING' COMMENT '实例状态：RUNNING=进行中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回 INITIATOR_CANCELED=已取消';

ALTER TABLE `tb_workflow_approval_record`
    MODIFY COLUMN `action` VARCHAR(16) NOT NULL COMMENT '操作类型：SUBMIT=提交申请 APPROVE=审批通过 REJECT=审批拒绝 DELEGATE=审批转交 RECALL=发起人撤回 CANCEL=发起人取消 ADD_SIGN=发起加签 ROUTE=系统自动路由 SPLIT_TRIGGER=系统触发并行拆分 JOIN_ARRIVE=分支到达并行聚合节点 JOIN_PASS=并行聚合完成并继续流转 AUTO_APPROVE=系统自动审核通过 AUTO_REJECT=系统自动审批拒绝 TIMEOUT=节点超时自动处理触发记录 REMIND=节点超时后发送提醒';

INSERT INTO `tb_sys_menu`
(`id`, `parent_id`, `type`, `name`, `permission`, `path`, `component`, `icon`, `sort_order`, `visible`, `status`, `created_at`, `updated_at`, `is_deleted`)
VALUES
(1000, 0, 'DIRECTORY', '系统管理', NULL, '/system', NULL, 'setting', 997, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1100, 1000, 'MENU', '用户管理', 'sys:user:list', '/system/user', 'pages/system/UserManagementPage', 'user', 10, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1101, 1100, 'BUTTON', '新增用户', 'sys:user:add', NULL, NULL, NULL, 10, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1102, 1100, 'BUTTON', '修改用户', 'sys:user:edit', NULL, NULL, NULL, 20, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1103, 1100, 'BUTTON', '删除用户', 'sys:user:delete', NULL, NULL, NULL, 30, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1200, 1000, 'MENU', '角色管理', 'sys:role:list', '/system/role', 'pages/system/RoleManagementPage', 'team', 20, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1201, 1200, 'BUTTON', '新增角色', 'sys:role:add', NULL, NULL, NULL, 10, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1202, 1200, 'BUTTON', '修改角色', 'sys:role:edit', NULL, NULL, NULL, 20, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1203, 1200, 'BUTTON', '删除角色', 'sys:role:delete', NULL, NULL, NULL, 30, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1300, 1000, 'MENU', '组织管理', 'sys:dept:tree', '/system/dept', 'pages/organization/DeptManagementPage', 'apartment', 30, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1301, 1300, 'BUTTON', '新增组织', 'sys:dept:add', NULL, NULL, NULL, 10, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1302, 1300, 'BUTTON', '修改组织', 'sys:dept:edit', NULL, NULL, NULL, 20, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1303, 1300, 'BUTTON', '删除组织', 'sys:dept:delete', NULL, NULL, NULL, 30, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1304, 1300, 'BUTTON', '移动组织', 'sys:dept:move', NULL, NULL, NULL, 40, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1400, 1000, 'MENU', '菜单管理', 'sys:menu:tree', '/system/menu', 'pages/system/MenuManagementPage', 'bars', 40, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1401, 1400, 'BUTTON', '新增菜单', 'sys:menu:add', NULL, NULL, NULL, 10, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1402, 1400, 'BUTTON', '修改菜单', 'sys:menu:edit', NULL, NULL, NULL, 20, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1403, 1400, 'BUTTON', '删除菜单', 'sys:menu:delete', NULL, NULL, NULL, 30, 1, 1, '2026-03-16 13:14:09', '2026-03-20 10:16:32', 0),
(1404, 0, 'DIRECTORY', '工作台', NULL, '/workbench', NULL, 'dashboard', 1, 1, 1, '2026-03-20 08:38:28', '2026-03-20 10:16:32', 0),
(1405, 1404, 'MENU', '业务办理', NULL, '/workbench/inbox', 'pages/workbench/InboxPage', 'inbox', 10, 1, 1, '2026-03-20 08:38:43', '2026-03-20 10:16:32', 0),
(1406, 1404, 'MENU', '待办箱', NULL, '/workbench/todo', 'pages/workbench/TodoPage', 'schedule', 20, 1, 1, '2026-03-20 08:42:29', '2026-03-20 10:16:32', 0),
(1407, 1404, 'MENU', '查询箱', NULL, '/workbench/query', 'pages/workbench/QueryPage', 'search', 30, 1, 1, '2026-03-20 08:42:42', '2026-03-20 10:16:32', 0),
(1408, 0, 'DIRECTORY', '组织机构', NULL, '/organization', NULL, 'apartment', 4, 1, 1, '2026-03-20 08:44:48', '2026-03-20 10:16:32', 0),
(1409, 1408, 'MENU', '组织管理', NULL, '/organization/dept', 'pages/organization/DeptManagementPage', 'apartment', 1, 1, 1, '2026-03-20 08:46:15', '2026-03-20 10:16:32', 0),
(1410, 0, 'MENU', '个人中心', NULL, '/profile', 'pages/profile/ProfilePage', 'profile', 999, 1, 1, '2026-03-20 08:56:48', '2026-03-20 10:16:32', 0),
(1411, 1100, 'BUTTON', '查询用户', 'sys:user:page', NULL, NULL, NULL, 1, 1, 1, '2026-03-20 09:06:20', '2026-03-20 10:16:32', 0),
(1412, 1200, 'BUTTON', '查询角色', 'sys:role:page', NULL, NULL, NULL, 1, 1, 1, '2026-03-20 09:12:58', '2026-03-20 10:16:32', 0),
(1413, 1300, 'BUTTON', '查询组织', 'system:dept:tree', NULL, NULL, NULL, 1, 1, 1, '2026-03-20 09:14:09', '2026-03-20 10:16:32', 0),
(1414, 1400, 'BUTTON', '菜单查询', 'system:menu;tree', NULL, NULL, NULL, 1, 1, 1, '2026-03-20 09:20:18', '2026-03-20 10:16:32', 0),
(1415, 0, 'DIRECTORY', '运营活动', NULL, '/operation', NULL, 'notification', 98, 1, 1, '2026-03-20 09:30:05', '2026-03-20 10:16:32', 0),
(1416, 1415, 'MENU', '系统公告', NULL, '/operation/notice', 'pages/operation/OperationListPage', 'notification', 1, 1, 1, '2026-03-20 09:30:57', '2026-03-20 10:16:32', 0),
(1417, 1404, 'MENU', '工作台首页', NULL, '/workbench/home', 'pages/workbench/WorkbenchPage', 'dashboard', 1, 1, 1, '2026-03-20 09:40:55', '2026-03-20 10:16:32', 0),
(1420, 0, 'DIRECTORY', '流程管理', NULL, '/workflow', NULL, 'workflow', 901, 1, 1, '2026-03-22 08:33:24', '2026-03-22 08:33:24', 0),
(1423, 1420, 'MENU', '流程列表', NULL, '/workflow/list', 'pages/workflow/ProcessListPage', 'workflow-instance', 2, 1, 1, '2026-03-22 09:32:36', '2026-03-22 09:32:36', 0),
(1424, 0, 'DIRECTORY', '业务建模', NULL, '/business', NULL, 'business-model', 900, 1, 1, '2026-03-23 03:46:21', '2026-03-23 03:46:21', 0),
(1425, 1424, 'MENU', '业务定义', NULL, '/business/list', '/pages/business/BusinessDefinitionPage', 'business-definition', 1, 1, 1, '2026-03-23 03:48:37', '2026-03-23 03:48:37', 0),
(1426, 1404, 'MENU', '草稿箱', NULL, '/workbench/draft', 'pages/workbench/DraftPage', 'profile', 15, 1, 1, '2026-03-23 12:22:20', '2026-03-23 12:22:20', 0),
(1427, 0, 'DIRECTORY', '系统日志', NULL, '/sys', NULL, 'log-center', 998, 1, 1, '2026-04-02 11:24:23', '2026-04-02 11:24:23', 0),
(1428, 1427, 'MENU', '登录日志', NULL, '/sys/loginlog', 'pages/system/LoginLogPage', 'login-log', 1, 1, 1, '2026-04-02 11:25:09', '2026-04-02 11:25:09', 0),
(1429, 1404, 'MENU', '已办箱', NULL, '/workbench/processed', 'pages/workbench/processedPage', 'workflow-done', 18, 1, 1, '2026-04-02 12:52:51', '2026-04-02 12:52:51', 0),
(1430, 1404, 'MENU', '我的发起', NULL, '/workbench/apply', 'pages/workbench/ApplyPage', 'inbox', 11, 1, 1, '2026-04-03 03:53:49', '2026-04-03 03:53:49', 0)
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
`status` = VALUES(`status`),
`updated_at` = VALUES(`updated_at`),
`is_deleted` = VALUES(`is_deleted`);

INSERT INTO `tb_user_role_menu`
(`id`, `role_id`, `menu_id`, `created_at`)
VALUES
(1, 1, 1000, '2026-04-03 12:00:00'),
(2, 1, 1100, '2026-04-03 12:00:00'),
(3, 1, 1101, '2026-04-03 12:00:00'),
(4, 1, 1102, '2026-04-03 12:00:00'),
(5, 1, 1103, '2026-04-03 12:00:00'),
(6, 1, 1200, '2026-04-03 12:00:00'),
(7, 1, 1201, '2026-04-03 12:00:00'),
(8, 1, 1202, '2026-04-03 12:00:00'),
(9, 1, 1203, '2026-04-03 12:00:00'),
(10, 1, 1300, '2026-04-03 12:00:00'),
(11, 1, 1301, '2026-04-03 12:00:00'),
(12, 1, 1302, '2026-04-03 12:00:00'),
(13, 1, 1303, '2026-04-03 12:00:00'),
(14, 1, 1304, '2026-04-03 12:00:00'),
(15, 1, 1400, '2026-04-03 12:00:00'),
(16, 1, 1401, '2026-04-03 12:00:00'),
(17, 1, 1402, '2026-04-03 12:00:00'),
(18, 1, 1403, '2026-04-03 12:00:00'),
(19, 1, 1404, '2026-04-03 12:00:00'),
(20, 1, 1405, '2026-04-03 12:00:00'),
(21, 1, 1406, '2026-04-03 12:00:00'),
(22, 1, 1407, '2026-04-03 12:00:00'),
(23, 1, 1408, '2026-04-03 12:00:00'),
(24, 1, 1409, '2026-04-03 12:00:00'),
(25, 1, 1410, '2026-04-03 12:00:00'),
(26, 1, 1411, '2026-04-03 12:00:00'),
(27, 1, 1412, '2026-04-03 12:00:00'),
(28, 1, 1413, '2026-04-03 12:00:00'),
(29, 1, 1414, '2026-04-03 12:00:00'),
(30, 1, 1415, '2026-04-03 12:00:00'),
(31, 1, 1416, '2026-04-03 12:00:00'),
(32, 1, 1417, '2026-04-03 12:00:00'),
(33, 1, 1420, '2026-04-03 12:00:00'),
(34, 1, 1423, '2026-04-03 12:00:00'),
(35, 1, 1424, '2026-04-03 12:00:00'),
(36, 1, 1425, '2026-04-03 12:00:00'),
(37, 1, 1426, '2026-04-03 12:00:00'),
(38, 1, 1427, '2026-04-03 12:00:00'),
(39, 1, 1428, '2026-04-03 12:00:00'),
(40, 1, 1429, '2026-04-03 12:00:00'),
(41, 1, 1430, '2026-04-03 12:00:00')
ON DUPLICATE KEY UPDATE
`role_id` = VALUES(`role_id`),
`menu_id` = VALUES(`menu_id`);

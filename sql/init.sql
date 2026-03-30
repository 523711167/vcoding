
-- 初始化数据仅保留 admin 用户、超级管理员角色、菜单数据及其必要关联数据。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `oauth2_authorization` (
  `id` VARCHAR(100) NOT NULL COMMENT '授权记录主键',
  `registered_client_id` VARCHAR(100) NOT NULL COMMENT 'OAuth2 客户端ID',
  `principal_name` VARCHAR(200) NOT NULL COMMENT '资源拥有者主体标识',
  `authorization_grant_type` VARCHAR(100) NOT NULL COMMENT '授权模式',
  `authorized_scopes` VARCHAR(1000) DEFAULT NULL COMMENT '授权范围集合',
  `attributes` BLOB DEFAULT NULL COMMENT '授权附加属性',
  `state` VARCHAR(500) DEFAULT NULL COMMENT '授权状态值',
  `authorization_code_value` BLOB DEFAULT NULL COMMENT '授权码值',
  `authorization_code_issued_at` TIMESTAMP DEFAULT NULL COMMENT '授权码签发时间',
  `authorization_code_expires_at` TIMESTAMP DEFAULT NULL COMMENT '授权码过期时间',
  `authorization_code_metadata` BLOB DEFAULT NULL COMMENT '授权码元数据',
  `access_token_value` BLOB DEFAULT NULL COMMENT '访问令牌值',
  `access_token_issued_at` TIMESTAMP DEFAULT NULL COMMENT '访问令牌签发时间',
  `access_token_expires_at` TIMESTAMP DEFAULT NULL COMMENT '访问令牌过期时间',
  `access_token_metadata` BLOB DEFAULT NULL COMMENT '访问令牌元数据',
  `access_token_type` VARCHAR(100) DEFAULT NULL COMMENT '访问令牌类型',
  `access_token_scopes` VARCHAR(1000) DEFAULT NULL COMMENT '访问令牌范围集合',
  `oidc_id_token_value` BLOB DEFAULT NULL COMMENT 'OIDC ID Token值',
  `oidc_id_token_issued_at` TIMESTAMP DEFAULT NULL COMMENT 'OIDC ID Token签发时间',
  `oidc_id_token_expires_at` TIMESTAMP DEFAULT NULL COMMENT 'OIDC ID Token过期时间',
  `oidc_id_token_metadata` BLOB DEFAULT NULL COMMENT 'OIDC ID Token元数据',
  `refresh_token_value` BLOB DEFAULT NULL COMMENT '刷新令牌值',
  `refresh_token_issued_at` TIMESTAMP DEFAULT NULL COMMENT '刷新令牌签发时间',
  `refresh_token_expires_at` TIMESTAMP DEFAULT NULL COMMENT '刷新令牌过期时间',
  `refresh_token_metadata` BLOB DEFAULT NULL COMMENT '刷新令牌元数据',
  `user_code_value` BLOB DEFAULT NULL COMMENT '设备授权用户码值',
  `user_code_issued_at` TIMESTAMP DEFAULT NULL COMMENT '设备授权用户码签发时间',
  `user_code_expires_at` TIMESTAMP DEFAULT NULL COMMENT '设备授权用户码过期时间',
  `user_code_metadata` BLOB DEFAULT NULL COMMENT '设备授权用户码元数据',
  `device_code_value` BLOB DEFAULT NULL COMMENT '设备授权设备码值',
  `device_code_issued_at` TIMESTAMP DEFAULT NULL COMMENT '设备授权设备码签发时间',
  `device_code_expires_at` TIMESTAMP DEFAULT NULL COMMENT '设备授权设备码过期时间',
  `device_code_metadata` BLOB DEFAULT NULL COMMENT '设备授权设备码元数据',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Spring Authorization Server OAuth2 授权持久化表';

CREATE TABLE IF NOT EXISTS `oauth2_authorization_consent` (
  `registered_client_id` VARCHAR(100) NOT NULL COMMENT 'OAuth2 客户端ID',
  `principal_name` VARCHAR(200) NOT NULL COMMENT '资源拥有者主体标识',
  `authorities` VARCHAR(1000) NOT NULL COMMENT '授权确认权限集合',
  PRIMARY KEY (`registered_client_id`, `principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Spring Authorization Server OAuth2 授权确认持久化表';

CREATE TABLE IF NOT EXISTS `tb_user_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父组织ID，顶级节点为0',
  `name` VARCHAR(100) NOT NULL COMMENT '组织名称',
  `code` VARCHAR(64) DEFAULT NULL COMMENT '组织编码（同级唯一）',
  `org_type` VARCHAR(16) NOT NULL DEFAULT 'DEPT' COMMENT '组织类型：GROUP/COMPANY/DEPT/POST',
  `post_type` VARCHAR(64) DEFAULT NULL COMMENT '岗位类型，org_type=POST时有值',
  `path` VARCHAR(500) NOT NULL COMMENT '祖级路径，格式：/1/5/12/',
  `level` INT NOT NULL DEFAULT 1 COMMENT '层级深度，顶级为1',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '同级排序值，升序展示',
  `leader_id` BIGINT DEFAULT NULL COMMENT '组织主管用户ID',
  `leader_name` VARCHAR(64) DEFAULT NULL COMMENT '组织主管姓名（冗余）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parent_code_deleted` (`parent_id`, `code`, `is_deleted`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_path` (`path`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织表';

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
  `dept_id` BIGINT NOT NULL COMMENT '组织ID',
  `org_type` VARCHAR(16) DEFAULT NULL COMMENT '组织类型，冗余自tb_user_dept.org_type',
  `post_type` VARCHAR(64) DEFAULT NULL COMMENT '岗位类型，冗余自tb_user_dept.post_type',
  `is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT '是否主组织：1=是 0=否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tb_user_dept` (`user_id`, `dept_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-组织直接关联表';

CREATE TABLE IF NOT EXISTS `tb_user_dept_rel_expand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `source_rel_id` BIGINT NOT NULL COMMENT '来源直接绑定关系ID',
  `source_dept_id` BIGINT NOT NULL COMMENT '来源直接绑定组织ID',
  `source_org_type` VARCHAR(16) NOT NULL COMMENT '来源组织类型冗余：GROUP/COMPANY/DEPT/POST',
  `source_post_type` VARCHAR(64) DEFAULT NULL COMMENT '来源岗位类型冗余；非岗位来源为空',
  `source_is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT '是否来源于主组织绑定：1=是 0=否',
  `dept_id` BIGINT NOT NULL COMMENT '展开后的目标组织ID',
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

CREATE TABLE IF NOT EXISTS `tb_user_role_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `dept_id` BIGINT NOT NULL COMMENT '组织ID',
  `org_type` VARCHAR(16) NOT NULL COMMENT '组织类型冗余：GROUP/COMPANY/DEPT/POST',
  `post_type` VARCHAR(64) DEFAULT NULL COMMENT '岗位类型冗余；仅当 org_type=POST 时有值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_dept` (`role_id`, `dept_id`),
  KEY `idx_role_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-自定义数据权限组织表';

CREATE TABLE IF NOT EXISTS `tb_user_role_dept_expand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `source_rel_id` BIGINT NOT NULL COMMENT '来源直接绑定关系ID',
  `source_dept_id` BIGINT NOT NULL COMMENT '来源直接绑定组织ID',
  `source_org_type` VARCHAR(16) NOT NULL COMMENT '来源组织类型冗余：GROUP/COMPANY/DEPT/POST',
  `source_post_type` VARCHAR(64) DEFAULT NULL COMMENT '来源岗位类型冗余；非岗位来源为空',
  `dept_id` BIGINT NOT NULL COMMENT '展开后的目标组织ID',
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

CREATE TABLE IF NOT EXISTS `tb_sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父节点ID，顶级节点为0',
  `type` VARCHAR(16) NOT NULL COMMENT '类型编码：DIRECTORY/MENU/BUTTON',
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

CREATE TABLE IF NOT EXISTS `tb_biz_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '业务编码（全局唯一）',
  `biz_name` VARCHAR(128) NOT NULL COMMENT '业务名称',
  `biz_desc` VARCHAR(500) DEFAULT NULL COMMENT '业务描述',
  `workflow_definition_id` BIGINT NOT NULL COMMENT '绑定的流程定义ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=停用',
  `created_by` BIGINT NOT NULL COMMENT '创建人用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_code` (`biz_code`),
  KEY `idx_workflow_definition_id` (`workflow_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务定义表';

CREATE TABLE IF NOT EXISTS `tb_biz_definition_role_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_definition_id` BIGINT NOT NULL COMMENT '业务定义ID（关联 tb_biz_definition.id）',
  `role_id` BIGINT NOT NULL COMMENT '角色ID（关联 tb_user_role.id）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_definition_role` (`biz_definition_id`, `role_id`),
  KEY `idx_biz_definition_id` (`biz_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务定义角色关联表';

CREATE TABLE IF NOT EXISTS `tb_biz_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_definition_id` BIGINT NOT NULL COMMENT '业务定义ID',
  `title` VARCHAR(200) NOT NULL COMMENT '申请标题',
  `biz_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '业务申请状态：DRAFT=草稿 PENDING=审批中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `applicant_name` VARCHAR(64) NOT NULL COMMENT '申请人姓名（冗余）',
  `dept_id` BIGINT DEFAULT NULL COMMENT '申请人所属组织ID（冗余）',
  `form_data` JSON DEFAULT NULL COMMENT '业务申请数据快照',
  `workflow_name` VARCHAR(100) DEFAULT NULL COMMENT '流程名称（冗余快照）',
  `workflow_instance_id` BIGINT DEFAULT NULL COMMENT '关联的审批工作流实例ID',
  `submitted_at` DATETIME DEFAULT NULL COMMENT '提交审批时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '审批完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_biz_definition_id_biz_status` (`biz_definition_id`, `biz_status`),
  KEY `idx_workflow_instance_id` (`workflow_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用业务申请表';

CREATE TABLE IF NOT EXISTS `tb_workflow_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '流程名称',
  `code` VARCHAR(64) NOT NULL COMMENT '流程编码（唯一标识）',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号，从1开始递增',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '流程描述',
  `workflow_json` LONGTEXT DEFAULT NULL COMMENT '前端流程设计原始JSON',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=已发布 2=已停用',
  `created_by` BIGINT NOT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_version` (`code`, `version`),
  KEY `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `definition_id` BIGINT NOT NULL COMMENT '所属流程定义ID',
  `name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `node_type` VARCHAR(32) NOT NULL COMMENT '节点类型：START/APPROVAL/CONDITION/PARALLEL_SPLIT/PARALLEL_JOIN/END',
  `parallel_split_node_id` BIGINT DEFAULT NULL COMMENT '所属最近一层并行拆分节点定义ID',
  `approve_mode` VARCHAR(16) DEFAULT NULL COMMENT '多人审批模式：AND/OR/SEQUENTIAL（仅APPROVAL节点有效）',
  `timeout_minutes` INT DEFAULT NULL COMMENT '超时时限（分钟）',
  `timeout_action` VARCHAR(16) DEFAULT NULL COMMENT '超时处理策略：AUTO_APPROVE/AUTO_REJECT/NOTIFY_ONLY',
  `remind_minutes` INT DEFAULT NULL COMMENT '提醒时限（分钟）',
  `position_x` INT NOT NULL DEFAULT 0 COMMENT '节点在画布上的X坐标',
  `position_y` INT NOT NULL DEFAULT 0 COMMENT '节点在画布上的Y坐标',
  `config_json` JSON DEFAULT NULL COMMENT '节点扩展配置JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_definition_id` (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_approver` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `definition_id` BIGINT NOT NULL COMMENT '所属流程定义ID',
  `node_id` BIGINT NOT NULL COMMENT '所属节点ID',
  `approver_type` VARCHAR(16) NOT NULL COMMENT '审批人类型：USER/ROLE/DEPT/INITIATOR_DEPT_LEADER',
  `approver_value` BIGINT NOT NULL COMMENT '审批人值：单个用户ID/角色ID/组织ID',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '顺序值（顺签场景有效）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点审批人配置表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_approver_dept_expand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `approver_id` BIGINT NOT NULL COMMENT '来源审批人配置ID',
  `node_id` BIGINT NOT NULL COMMENT '来源流程节点ID',
  `definition_id` BIGINT NOT NULL COMMENT '来源流程定义ID',
  `source_dept_id` BIGINT NOT NULL COMMENT '来源直接配置组织ID',
  `source_org_type` VARCHAR(32) NOT NULL COMMENT '来源组织类型冗余',
  `source_post_type` VARCHAR(64) DEFAULT NULL COMMENT '来源岗位类型冗余，非岗位来源时为空',
  `dept_id` BIGINT NOT NULL COMMENT '展开后的目标组织ID',
  `org_type` VARCHAR(32) NOT NULL COMMENT '目标组织类型冗余',
  `post_type` VARCHAR(64) DEFAULT NULL COMMENT '目标岗位类型冗余，非岗位目标时为空',
  `relation_type` VARCHAR(16) NOT NULL COMMENT '关系类型：SELF/DESCENDANT',
  `distance` INT NOT NULL DEFAULT 0 COMMENT '展开距离，自身为0，直接下级为1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approver_dept` (`approver_id`, `dept_id`),
  KEY `idx_node_id` (`node_id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_source_dept_id` (`source_dept_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点审批组织展开关系表';

CREATE TABLE IF NOT EXISTS `tb_workflow_transition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `definition_id` BIGINT NOT NULL COMMENT '所属流程定义ID',
  `from_node_id` BIGINT NOT NULL COMMENT '来源节点ID',
  `from_node_name` VARCHAR(100) DEFAULT NULL COMMENT '来源节点名称（冗余）',
  `from_node_type` VARCHAR(32) DEFAULT NULL COMMENT '来源节点类型（冗余）',
  `to_node_id` BIGINT NOT NULL COMMENT '目标节点ID',
  `to_node_name` VARCHAR(100) DEFAULT NULL COMMENT '目标节点名称（冗余）',
  `to_node_type` VARCHAR(32) DEFAULT NULL COMMENT '目标节点类型（冗余）',
  `condition_expr` VARCHAR(512) DEFAULT NULL COMMENT '条件表达式',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认分支：0=否 1=是',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '条件优先级（值越小越先判断）',
  `label` VARCHAR(64) DEFAULT NULL COMMENT '连线标签',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_definition_from` (`definition_id`, `from_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流转条件表';

CREATE TABLE IF NOT EXISTS `tb_workflow_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_id` BIGINT NOT NULL COMMENT '业务定义ID',
  `definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `definition_code` VARCHAR(64) NOT NULL COMMENT '流程编码（冗余，方便查询）',
  `title` VARCHAR(200) NOT NULL COMMENT '审批标题',
  `status` VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT '实例状态：RUNNING=进行中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `applicant_name` VARCHAR(64) NOT NULL COMMENT '申请人姓名（冗余）',
  `form_data` JSON DEFAULT NULL COMMENT '业务申请数据快照',
  `current_node_id` BIGINT DEFAULT NULL COMMENT '当前所在节点定义ID',
  `current_node_name` VARCHAR(100) DEFAULT NULL COMMENT '当前所在节点定义名称（冗余）',
  `current_node_type` VARCHAR(32) DEFAULT NULL COMMENT '当前所在节点定义类型（冗余）',
  `started_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_biz_id` (`biz_id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` BIGINT NOT NULL COMMENT '所属流程实例ID',
  `definition_node_id` BIGINT NOT NULL COMMENT '节点定义ID',
  `definition_node_name` VARCHAR(100) NOT NULL COMMENT '节点定义名称（冗余）',
  `definition_node_type` VARCHAR(32) NOT NULL COMMENT '节点定义类型（冗余）',
  `parallel_branch_root_id` BIGINT DEFAULT NULL COMMENT '所属最近一层并行拆分节点实例ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待激活 ACTIVE=进行中 PENDING_APPROVAL=待定/审批中 APPROVED=已通过 REJECTED=已拒绝 SKIPPED=已跳过 CANCELED=已取消 TIMEOUT=已超时',
  `approve_mode` VARCHAR(16) DEFAULT NULL COMMENT '审批模式（冗余）',
  `activated_at` DATETIME DEFAULT NULL COMMENT '节点激活时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '节点完成时间',
  `deadline_at` DATETIME DEFAULT NULL COMMENT '超时截止时间',
  `remind_at` DATETIME DEFAULT NULL COMMENT '催办提醒时间',
  `is_reminded` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送催办通知',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '节点备注（如自动处理原因）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_status_deadline` (`status`, `deadline_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_node_approver_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_instance_id` BIGINT NOT NULL COMMENT '所属节点实例ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID（冗余，方便查询）',
  `approver_id` BIGINT NOT NULL COMMENT '审批人用户ID',
  `approver_name` VARCHAR(64) NOT NULL COMMENT '审批人姓名（冗余）',
  `node_name` VARCHAR(100) NOT NULL COMMENT '所属节点名称（冗余）',
  `node_type` VARCHAR(32) NOT NULL COMMENT '所属节点类型（冗余）',
  `relation_type` VARCHAR(16) NOT NULL DEFAULT 'ORIGINAL' COMMENT '来源关系类型：ORIGINAL=原始审批人 ADD_SIGN=加签审批人',
  `source_approver_instance_id` BIGINT DEFAULT NULL COMMENT '来源审批人实例ID（加签审批人时指向发起加签的审批人实例）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '顺签顺序',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待处理 WAITING_ADD_SIGN=等待加签 APPROVED=已通过 REJECTED=已拒绝 DELEGATED=已转交 CANCELED=已取消',
  `is_active` TINYINT NOT NULL DEFAULT 0 COMMENT '是否当前需要操作（顺签场景下只有当前人为1）',
  `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `delegate_to` BIGINT DEFAULT NULL COMMENT '转交目标用户ID（DELEGATED状态时有值）',
  `delegate_to_name` VARCHAR(64) DEFAULT NULL COMMENT '转交目标用户姓名（冗余）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_node_instance_id` (`node_instance_id`),
  KEY `idx_approver_id_status` (`approver_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点审批人实例表';

CREATE TABLE IF NOT EXISTS `tb_workflow_approval_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
  `node_instance_id` BIGINT NOT NULL COMMENT '节点实例ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人用户ID',
  `operator_name` VARCHAR(64) NOT NULL COMMENT '操作人姓名',
  `action` VARCHAR(16) NOT NULL COMMENT '操作类型：SUBMIT=提交申请 APPROVE=审批通过 REJECT=审批拒绝 DELEGATE=审批转交 RECALL=发起人撤回 ADD_SIGN=发起加签 ROUTE=系统自动路由 SPLIT_TRIGGER=系统触发并行拆分 JOIN_ARRIVE=分支到达并行聚合节点 JOIN_PASS=并行聚合完成并继续流转 AUTO_APPROVE=系统自动审核通过 AUTO_REJECT=系统自动审批拒绝 TIMEOUT=节点超时自动处理触发记录 REMIND=节点超时后发送提醒',
  `node_instance_type` VARCHAR(32) DEFAULT NULL COMMENT '节点实例类型（冗余）',
  `node_instance_name` VARCHAR(100) DEFAULT NULL COMMENT '节点实例名称（冗余）',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '操作备注',
  `from_node_id` BIGINT DEFAULT NULL COMMENT '来源节点实例ID',
  `from_node_type` VARCHAR(32) DEFAULT NULL COMMENT '来源节点实例类型（冗余）',
  `from_node_name` VARCHAR(100) DEFAULT NULL COMMENT '来源节点实例名称（冗余）',
  `to_node_id` BIGINT DEFAULT NULL COMMENT '目标节点实例ID',
  `to_node_type` VARCHAR(32) DEFAULT NULL COMMENT '目标节点实例类型（冗余）',
  `to_node_name` VARCHAR(100) DEFAULT NULL COMMENT '目标节点实例名称（冗余）',
  `extra_data` JSON DEFAULT NULL COMMENT '附加数据（如附件列表、转交目标等）',
  `operated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作记录表';

INSERT INTO `tb_sys_menu`
(`id`, `parent_id`, `type`, `name`, `permission`, `path`, `component`, `icon`, `sort_order`, `visible`, `status`)
VALUES
(1417, 0, 'MENU', '首页', NULL, '/dashboard', 'dashboard/index', 'dashboard', 1, 1, 1),
(1404, 0, 'DIRECTORY', '工作台', NULL, '/workbench', NULL, 'home', 5, 1, 1),
(1405, 1404, 'MENU', '收件箱', NULL, '/workbench/inbox', 'workbench/inbox/index', 'inbox', 10, 1, 1),
(1406, 1404, 'MENU', '我的待办', NULL, '/workbench/todo', 'workbench/todo/index', 'todo', 20, 1, 1),
(1407, 1404, 'MENU', '任务查询', NULL, '/workbench/query', 'workbench/query/index', 'search', 30, 1, 1),
(1408, 0, 'DIRECTORY', '组织中心', NULL, '/organization', NULL, 'tree', 6, 1, 1),
(1409, 1408, 'MENU', '组织架构', NULL, '/organization/dept', 'organization/dept/index', 'tree', 10, 1, 1),
(1410, 0, 'MENU', '个人中心', NULL, '/profile', 'profile/index', 'user', 90, 1, 1),
(1415, 0, 'DIRECTORY', '运营管理', NULL, '/operation', NULL, 'operation', 70, 1, 1),
(1416, 1415, 'MENU', '通知公告', NULL, '/operation/notice', 'operation/notice/index', 'notice', 10, 1, 1),
(1000, 0, 'DIRECTORY', '系统管理', NULL, '/system', NULL, 'setting', 10, 1, 1),
(1100, 1000, 'MENU', '用户管理', 'sys:user:list', '/system/user', 'system/user/index', 'user', 10, 1, 1),
(1101, 1100, 'BUTTON', '新增用户', 'sys:user:add', NULL, NULL, NULL, 10, 1, 1),
(1102, 1100, 'BUTTON', '修改用户', 'sys:user:edit', NULL, NULL, NULL, 20, 1, 1),
(1103, 1100, 'BUTTON', '删除用户', 'sys:user:delete', NULL, NULL, NULL, 30, 1, 1),
(1104, 1100, 'BUTTON', '重置密码', 'sys:user:reset-pwd', NULL, NULL, NULL, 40, 1, 1),
(1105, 1100, 'BUTTON', '分配角色', 'sys:user:assign-role', NULL, NULL, NULL, 50, 1, 1),
(1106, 1100, 'BUTTON', '分配组织', 'sys:user:assign-dept', NULL, NULL, NULL, 60, 1, 1),
(1200, 1000, 'MENU', '角色管理', 'sys:role:list', '/system/role', 'system/role/index', 'peoples', 20, 1, 1),
(1201, 1200, 'BUTTON', '新增角色', 'sys:role:add', NULL, NULL, NULL, 10, 1, 1),
(1202, 1200, 'BUTTON', '修改角色', 'sys:role:edit', NULL, NULL, NULL, 20, 1, 1),
(1203, 1200, 'BUTTON', '删除角色', 'sys:role:delete', NULL, NULL, NULL, 30, 1, 1),
(1204, 1200, 'BUTTON', '分配菜单', 'sys:role:assign-menu', NULL, NULL, NULL, 40, 1, 1),
(1205, 1200, 'BUTTON', '数据权限', 'sys:role:data-scope', NULL, NULL, NULL, 50, 1, 1),
(1300, 1000, 'MENU', '组织管理', 'sys:dept:tree', '/system/dept', 'system/dept/index', 'tree', 30, 1, 1),
(1301, 1300, 'BUTTON', '新增组织', 'sys:dept:add', NULL, NULL, NULL, 10, 1, 1),
(1302, 1300, 'BUTTON', '修改组织', 'sys:dept:edit', NULL, NULL, NULL, 20, 1, 1),
(1303, 1300, 'BUTTON', '删除组织', 'sys:dept:delete', NULL, NULL, NULL, 30, 1, 1),
(1304, 1300, 'BUTTON', '移动组织', 'sys:dept:move', NULL, NULL, NULL, 40, 1, 1),
(1400, 1000, 'MENU', '菜单管理', 'sys:menu:tree', '/system/menu', 'system/menu/index', 'menu', 40, 1, 1),
(1401, 1400, 'BUTTON', '新增菜单', 'sys:menu:add', NULL, NULL, NULL, 10, 1, 1),
(1402, 1400, 'BUTTON', '修改菜单', 'sys:menu:edit', NULL, NULL, NULL, 20, 1, 1),
(1403, 1400, 'BUTTON', '删除菜单', 'sys:menu:delete', NULL, NULL, NULL, 30, 1, 1)
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
(`id`, `parent_id`, `name`, `code`, `org_type`, `post_type`, `path`, `level`, `sort_order`, `leader_id`, `leader_name`, `status`)
VALUES
(1, 0, '总部', 'HEAD_OFFICE', 'GROUP', NULL, '/1/', 1, 10, 1, '超级管理员', 1)
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`name` = VALUES(`name`),
`code` = VALUES(`code`),
`org_type` = VALUES(`org_type`),
`post_type` = VALUES(`post_type`),
`path` = VALUES(`path`),
`level` = VALUES(`level`),
`sort_order` = VALUES(`sort_order`),
`leader_id` = VALUES(`leader_id`),
`leader_name` = VALUES(`leader_name`),
`status` = VALUES(`status`);

INSERT INTO `tb_user_role`
(`id`, `name`, `code`, `description`, `status`, `sort_order`, `data_scope`)
VALUES
(1, '超级管理员', 'ADMIN', '拥有系统全部管理权限', 1, 10, 'ALL')
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
(1, 'admin', '$2a$10$Hf3Pqi4aXMTReDLGi/W9a.0I2ohxTWUMWxbbwgdxcWmVe1vF4HbPO', '超级管理员', 'admin@yuyu.com', '13800000000', NULL, 1, NULL)
ON DUPLICATE KEY UPDATE
`username` = VALUES(`username`),
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`email` = VALUES(`email`),
`mobile` = VALUES(`mobile`),
`avatar` = VALUES(`avatar`),
`status` = VALUES(`status`),
`last_login_at` = VALUES(`last_login_at`);

INSERT INTO `tb_user_role_rel`
(`id`, `user_id`, `role_id`)
VALUES
(1, 1, 1)
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`role_id` = VALUES(`role_id`);

INSERT INTO `tb_user_dept_rel`
(`id`, `user_id`, `dept_id`, `org_type`, `post_type`, `is_primary`)
VALUES
(1, 1, 1, 'GROUP', NULL, 1)
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`dept_id` = VALUES(`dept_id`),
`org_type` = VALUES(`org_type`),
`post_type` = VALUES(`post_type`),
`is_primary` = VALUES(`is_primary`);

INSERT INTO `tb_user_dept_rel_expand`
(`id`, `user_id`, `source_rel_id`, `source_dept_id`, `source_org_type`, `source_post_type`, `source_is_primary`, `dept_id`, `org_type`, `post_type`, `relation_type`, `distance`)
VALUES
(1, 1, 1, 1, 'GROUP', NULL, 1, 1, 'GROUP', NULL, 'SELF', 0)
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`source_rel_id` = VALUES(`source_rel_id`),
`source_dept_id` = VALUES(`source_dept_id`),
`source_org_type` = VALUES(`source_org_type`),
`source_post_type` = VALUES(`source_post_type`),
`source_is_primary` = VALUES(`source_is_primary`),
`dept_id` = VALUES(`dept_id`),
`org_type` = VALUES(`org_type`),
`post_type` = VALUES(`post_type`),
`relation_type` = VALUES(`relation_type`),
`distance` = VALUES(`distance`);

INSERT INTO `tb_user_role_menu`
(`id`, `role_id`, `menu_id`)
VALUES
(1, 1, 1417),
(2, 1, 1404),
(3, 1, 1405),
(4, 1, 1406),
(5, 1, 1407),
(6, 1, 1408),
(7, 1, 1409),
(8, 1, 1410),
(9, 1, 1415),
(10, 1, 1416),
(11, 1, 1000),
(12, 1, 1100),
(13, 1, 1101),
(14, 1, 1102),
(15, 1, 1103),
(16, 1, 1104),
(17, 1, 1105),
(18, 1, 1106),
(19, 1, 1200),
(20, 1, 1201),
(21, 1, 1202),
(22, 1, 1203),
(23, 1, 1204),
(24, 1, 1205),
(25, 1, 1300),
(26, 1, 1301),
(27, 1, 1302),
(28, 1, 1303),
(29, 1, 1304),
(30, 1, 1400),
(31, 1, 1401),
(32, 1, 1402),
(33, 1, 1403)
ON DUPLICATE KEY UPDATE
`role_id` = VALUES(`role_id`),
`menu_id` = VALUES(`menu_id`);

-- 变更原因：
-- 1. OAuth2 access token / refresh token / revoke / introspect 需要跨应用重启持久化。
-- 2. Spring Authorization Server 内置授权表按框架官方表名与官方表结构落库，避免与框架 JDBC 实现产生偏差。

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

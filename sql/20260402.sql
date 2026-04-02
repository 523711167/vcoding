-- 变更原因：
-- 1. 新增 tb_login_log，记录用户名密码登录成功/失败留痕，满足登录审计追踪要求。

CREATE TABLE IF NOT EXISTS `tb_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '登录用户ID，失败场景为空',
  `username` VARCHAR(64) DEFAULT NULL COMMENT '登录用户名',
  `result` VARCHAR(16) NOT NULL COMMENT '登录结果：SUCCESS/FAIL',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP地址',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '客户端User-Agent',
  `login_at` DATETIME NOT NULL COMMENT '登录发生时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_result` (`result`),
  KEY `idx_login_at` (`login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';

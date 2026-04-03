-- 变更原因：新增发起人取消流程能力，保留原撤回语义不变，补充动作与终态注释，并为业务申请增加取消原因字段。

ALTER TABLE `tb_biz_apply`
    ADD COLUMN `cancel_reason` VARCHAR(500) NULL COMMENT '发起人取消原因，仅发起人取消流程时有值' AFTER `workflow_instance_id`;

ALTER TABLE `tb_biz_apply`
    MODIFY COLUMN `biz_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '业务申请状态：DRAFT=草稿 PENDING=审批中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回 INITIATOR_CANCELED=已取消';

ALTER TABLE `tb_workflow_instance`
    MODIFY COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'RUNNING' COMMENT '实例状态：RUNNING=进行中 APPROVED=已通过 REJECTED=已拒绝 CANCELED=已撤回 INITIATOR_CANCELED=已取消';

ALTER TABLE `tb_workflow_approval_record`
    MODIFY COLUMN `action` VARCHAR(16) NOT NULL COMMENT '操作类型：SUBMIT=提交申请 APPROVE=审批通过 REJECT=审批拒绝 DELEGATE=审批转交 RECALL=发起人撤回 CANCEL=发起人取消 ADD_SIGN=发起加签 ROUTE=系统自动路由 SPLIT_TRIGGER=系统触发并行拆分 JOIN_ARRIVE=分支到达并行聚合节点 JOIN_PASS=并行聚合完成并继续流转 AUTO_APPROVE=系统自动审核通过 AUTO_REJECT=系统自动审批拒绝 TIMEOUT=节点超时自动处理触发记录 REMIND=节点超时后发送提醒';

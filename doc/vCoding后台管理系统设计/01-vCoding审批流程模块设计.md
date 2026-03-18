# vCoding 审批流程模块设计

## 1. 设计范围

本文件仅聚焦审批工作流本身的功能设计，包括流程定义、节点、连线、审批人、流程实例、节点实例、审批操作、版本发布和运行机制。

## 2. 核心概念

| 概念 | 说明 |
|------|------|
| 流程定义 | 可复用的流程模板 |
| 流程实例 | 某次具体审批的运行记录 |
| 节点 | 流程中的处理单元 |
| 流转 | 节点间的有向边，可附带条件表达式 |
| 审批人 | 在节点执行审批操作的人 |

## 3. 节点与审批人类型

### 3.1 节点类型

| 类型值 | 名称 | 说明 |
|--------|------|------|
| `START` | 开始节点 | 流程唯一入口 |
| `APPROVAL` | 审批节点 | 需要审批人处理 |
| `CONDITION` | 条件节点 | 根据条件表达式决定走向 |
| `PARALLEL_SPLIT` | 并行拆分 | 同时激活多条分支 |
| `PARALLEL_JOIN` | 并行聚合 | 等待全部分支完成 |
| `END` | 结束节点 | 流程终止节点 |

### 3.2 审批人类型

| 值 | 说明 |
|----|------|
| `USER` | 指定具体用户 |
| `ROLE` | 指定角色，运行时解析有效用户 |
| `DEPT` | 指定组织，运行时解析候选审批人 |
| `INITIATOR_DEPT_LEADER` | 解析发起人主组织主管 |

## 4. 定义层设计

定义层核心表：
- `tb_workflow_definition`
- `tb_workflow_node`
- `tb_workflow_node_approver`
- `tb_workflow_transition`

关键约束：
- 同一 `code` 下只能有一个已发布版本。
- `APPROVAL` 节点必须至少配置一个审批人。
- 同一流程定义下 `node_key` 必须唯一。
- 条件分支建议保留兜底路径，避免流程卡死。

前端设计器映射建议：
- `definition` 对应 `tb_workflow_definition`
- `nodes` 对应 `tb_workflow_node`
- `transitions` 对应 `tb_workflow_transition`
- `approvers` 对应 `tb_workflow_node_approver`

## 5. 运行层设计

运行层核心表：
- `tb_workflow_instance`
- `tb_workflow_node_instance`
- `tb_workflow_node_approver_instance`
- `tb_workflow_approval_record`

职责划分：
- `tb_workflow_instance` 记录流程整体状态。
- `tb_workflow_node_instance` 记录节点运行状态。
- `tb_workflow_node_approver_instance` 记录每个审批人的处理状态。
- `tb_workflow_approval_record` 记录不可修改的审计流水。

## 6. 发布与版本机制

- 草稿流程允许反复编辑。
- 已发布流程不得直接修改，必须复制出新草稿版本。
- 发布动作必须放在同一事务内完成版本切换。
- 流程实例创建后绑定具体 `definition_id`，历史实例不因新版本发布而受影响。

## 7. 发起审批流程

前置校验：
- 业务类型存在且启用。
- 业务类型已配置有效 `workflow_definition_id`。
- 当前用户具备业务发起权限。

发起步骤：
1. 根据 `biz_code` 查询业务类型并取得流程定义。
2. 创建 `tb_workflow_instance`。
3. 从 `START` 节点出发，激活第一个有效节点。
4. 创建节点实例与审批人实例。
5. 写入 `SUBMIT` 审批记录。

## 8. 节点流转机制

- 当前节点处理完成后，按 `priority` 升序查询所有出边。
- 依次计算 `condition_expr`。
- 第一条命中的连线生效。
- 若命中 `END`，流程结束。
- 若命中 `PARALLEL_SPLIT`，同时激活所有分支。
- 若命中 `PARALLEL_JOIN`，等待全部预期分支完成后继续。

## 9. 多人审批模式

### 9.1 `AND`

- 所有人都通过，节点才通过。
- 任意一人拒绝，节点立即拒绝。

### 9.2 `OR`

- 任意一人通过，节点立即通过。
- 所有人拒绝，节点才拒绝。

### 9.3 `SEQUENTIAL`

- 按 `sort_order` 依次激活审批人。
- 任意一人拒绝，节点立即拒绝。
- 所有人依次通过，节点才通过。

## 10. 条件分支、并行与超时

条件表达式变量来源：
- `tb_workflow_instance.form_data`
- `initiator_id`
- `initiator_dept_id`
- `current_time`

并行处理规则：
- `PARALLEL_SPLIT` 负责拆分。
- `PARALLEL_JOIN` 负责聚合。
- `config_json.expect_branch_count` 记录预期分支数。

超时策略：
- `AUTO_APPROVE`
- `AUTO_REJECT`
- `NOTIFY_ONLY`

## 11. 审批操作与通知

支持操作：
- `APPROVE`
- `REJECT`
- `DELEGATE`
- `RECALL`
- `URGE`
- 超时自动处理

通知触发点：
- 节点激活
- 节点被拒绝
- 流程完成
- 催办提醒
- 超时自动处理

## 12. 状态机

流程实例状态建议：
- `RUNNING`
- `APPROVED`
- `REJECTED`
- `CANCELED`

节点实例状态建议：
- `PENDING`
- `ACTIVE`
- `APPROVED`
- `REJECTED`
- `SKIPPED`
- `TIMEOUT`

## 13. 与其他模块的衔接

- 与业务类型通过 `tb_biz_type.workflow_definition_id` 绑定。
- 与发起权限通过 `tb_biz_type_initiator` 衔接。
- 与用户、角色、组织通过审批人解析规则衔接。
- 与字典、业务表单通过 `form_data` 和条件表达式衔接。

完整 SQL、流程示例和原始章节可查看 [vCoding 后台管理系统完整设计归档](./00-vCoding后台管理系统完整设计归档.md)。

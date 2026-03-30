# vCoding 审批流程模块设计

## 1. 设计范围

本文件仅描述审批工作流模块本身，不展开权限体系、业务模型和系统基础数据设计。覆盖范围包括：
- 流程定义
- 节点与连线
- 审批人配置与解析
- 流程实例运行
- 节点实例与审批记录
- 版本发布与运行机制

## 2. 设计目标

- 建立一套可复用、可发布、可追溯的流程定义模型。
- 支撑请假、报销、合同等多业务场景复用同一套流程引擎。
- 保证流程发布、实例运行、审批审计三层数据职责清晰。
- 保证流程升级不影响历史实例的运行和回溯。

## 3. 核心概念

| 概念 | 说明 |
|------|------|
| 流程定义 | 可复用的流程模板，描述节点、连线、审批人和版本信息 |
| 流程实例 | 某次具体业务提交后产生的运行记录 |
| 节点 | 流程中的处理单元，如开始、审批、条件、并行、结束 |
| 流转 | 节点之间的有向边，可附带条件表达式与优先级 |
| 审批人 | 节点激活后参与审批处理的用户集合 |

## 4. 节点与审批人模型

### 4.1 节点类型

| 类型值 | 名称 | 说明 |
|--------|------|------|
| `START` | 开始节点 | 流程唯一入口 |
| `APPROVAL` | 审批节点 | 需要审批人处理的业务节点 |
| `CONDITION` | 条件节点 | 根据条件表达式决定流向 |
| `PARALLEL_SPLIT` | 并行拆分 | 同时激活多条分支 |
| `PARALLEL_JOIN` | 并行聚合 | 等待全部预期分支完成 |
| `END` | 结束节点 | 流程终止节点 |

### 4.2 审批人类型

| 类型值 | 说明 |
|--------|------|
| `USER` | 指定具体用户 |
| `ROLE` | 指定角色，运行时解析有效用户 |
| `DEPT` | 指定组织，运行时解析候选审批人 |
| `INITIATOR_DEPT_LEADER` | 动态解析发起人主组织主管 |

## 5. 定义层设计

### 5.1 核心表

- `tb_workflow_definition`
- `tb_workflow_node`
- `tb_workflow_node_approver`
- `tb_workflow_node_approver_dept_expand`
- `tb_workflow_transition`

### 5.2 职责划分

- `tb_workflow_definition`：保存流程定义主信息、版本、发布状态，以及前端流程设计原始 JSON。
- `tb_workflow_node`：保存流程节点结构、节点类型、节点配置，并维护节点所属最近一层并行拆分节点定义 ID。
- `tb_workflow_node_approver`：保存审批节点对应的审批人直接配置，一条记录只对应一个审批主体，并冗余 `definition_id`。
- `tb_workflow_node_approver_dept_expand`：保存 `DEPT` 类型审批人的向下展开结果，仅作为派生生效范围表使用。
- `tb_workflow_transition`：保存节点之间的流转关系、优先级、条件表达式、默认分支标记，以及来源/目标节点名称和类型冗余。

### 5.3 关键约束

- 同一 `code` 下任一时刻只能有一个已发布版本。
- `tb_workflow_definition` 不再冗余保存 `biz_code`，流程与业务的绑定统一由 `tb_biz_definition.workflow_definition_id` 维护。
- 同一流程定义下前端节点 `id` 必须唯一，但该 `id` 仅用于保存时解析，不再单独落库。
- `tb_workflow_node.parallel_split_node_id` 由后端保存流程时自动计算，表示当前定义节点所属最近一层 `PARALLEL_SPLIT` 节点定义 ID。
- `APPROVAL` 节点必须至少配置一个审批人。
- 审批人 direct 配置必须满足“一条记录一个审批主体”，`approver_value` 使用 `BIGINT` 保存单个主体 ID，不允许逗号拼接多个值。
- `DEPT` 类型审批人除 direct 配置外，还需要维护组织展开表，且只保留当前有效组织节点。
- 节点超时与提醒时长统一使用“分钟”作为存储和接口口径。
- `CONDITION` 与 `PARALLEL_SPLIT` 节点在存在多条出边时，必须且只能配置一条默认分支，避免流程停滞。

### 5.4 前端设计器映射建议

- `definition` 对应 `tb_workflow_definition`
  流程设计器提交的整份 JSON 额外原样保存到 `tb_workflow_definition.workflow_json`，用于详情回显和前端直接渲染。
- `nodes` 对应 `tb_workflow_node`
  后端在保存时解析前端节点 `id`、`properties`、`text`，拆分并落到节点、审批人、连线表。
- `transitions` 对应 `tb_workflow_transition`
  后端在保存时解析边上的 `properties.expression`、`properties.priority`、`properties.is_default`，分别落到 `condition_expr`、`priority`、`is_default`，并同步冗余 `from_node_name/from_node_type/to_node_name/to_node_type`。
- `approvers` 对应 `tb_workflow_node_approver`
  审批人保存时按数组逐项拆分，一条审批主体写一条 direct 记录；当 `approverType=DEPT` 时，再同步重建 `tb_workflow_node_approver_dept_expand`。

## 6. 运行层设计

### 6.1 核心表

- `tb_workflow_instance`
- `tb_workflow_node_instance`
- `tb_workflow_node_approver_instance`
- `tb_workflow_approval_record`

### 6.2 职责划分

- `tb_workflow_instance`：记录流程整体运行状态，并冗余业务定义和当前节点快照。
- `tb_workflow_node_instance`：记录节点运行状态和节点级上下文，节点字段统一表达“节点定义快照”。
- `tb_workflow_node_approver_instance`：记录每个审批人的待办与处理状态，并冗余所属节点名称、类型和转交目标姓名。
- `tb_workflow_approval_record`：记录不可修改的审批审计流水。

### 6.3 运行层关键字段补充

- `tb_workflow_instance.biz_id`：冗余业务定义 ID，对应 `tb_biz_definition.id`。
- `tb_workflow_instance.current_node_id/current_node_name/current_node_type`：记录流程当前主运行位置的节点定义快照。
- `tb_workflow_node_instance.definition_node_id/definition_node_name/definition_node_type`：统一表达当前节点实例对应的节点定义快照。
- `tb_workflow_node_instance.parallel_branch_root_id`：记录当前节点实例所属最近一层并行拆分节点实例 ID，非并行分支为空。
- `tb_workflow_node_approver_instance.finished_at`：统一表达审批人实例完成时间。
- `tb_workflow_node_approver_instance.delegate_to_name`：冗余转交目标用户姓名。
- `tb_workflow_approval_record` 需要冗余 `node_instance_*`、`from_node_*`、`to_node_*` 三组节点名称和类型字段。

## 7. 发布与版本机制

- 草稿流程允许反复编辑。
- 已发布流程编辑时，不直接覆盖原记录，而是基于最新内容生成新的草稿版本。
- 已停用流程编辑时，与已发布流程保持同一处理逻辑，生成新的草稿版本。
- 新草稿保存后，旧的已发布版本保持不变；只有手动发布新草稿时，旧发布版本才自动转为已停用。
- 发布动作必须在同一事务内完成版本切换。
- 流程实例创建后固定绑定 `definition_id`，不受后续新版本发布影响。

## 8. 发起流程

### 8.1 前置校验

- 业务定义存在且处于启用状态。
- 业务定义已配置有效的 `workflow_definition_id`。
- 当前用户具备对应业务的发起权限。

### 8.2 发起步骤

1. 根据 `tb_biz_apply.biz_definition_id` 查询业务定义并定位已发布流程定义。
2. 创建 `tb_workflow_instance`，同步写入 `biz_id`、`definition_id`、`definition_code`、申请人快照和业务表单快照。
3. 从 `START` 节点出发，解析并激活首个有效节点。
4. 按实际进入运行的节点创建 `tb_workflow_node_instance`；`START`、`END` 不创建节点实例。
5. 若首批进入的是审批节点，则创建 `tb_workflow_node_approver_instance`。
6. 回写 `tb_biz_apply.workflow_instance_id`、`workflow_name`、`submitted_at`、`biz_status`。
7. 写入 `SUBMIT` 审批记录，以及必要的系统 `ROUTE` / `SPLIT_TRIGGER` 记录。

补充说明：
- 业务侧通过 `tb_biz_definition.workflow_definition_id` 选择流程定义版本。
- 流程定义表本身不再保存 `biz_code`。
- 业务申请表与业务定义的关联统一使用主键 `biz_definition_id`，不再通过 `biz_code` 反查。

## 9. 节点流转机制

- 当前节点处理完成后，按 `priority` 升序读取所有出边。
- `CONDITION` 节点按优先级依次计算非默认分支的 `condition_expr`，第一条命中的连线生效。
- `CONDITION` 节点若无条件命中，则走唯一默认分支；若不存在默认分支，则按流程配置错误处理。
- 命中 `END` 时，流程实例结束。
- `PARALLEL_SPLIT` 节点遍历全部非默认分支，激活所有命中的分支。
- `PARALLEL_SPLIT` 节点若没有任何条件分支命中，则走唯一默认分支；若不存在默认分支，则按流程配置错误处理。
- 命中 `PARALLEL_JOIN` 时，等待全部预期分支完成后继续。
- 并行分支中节点实例通过 `parallel_branch_root_id` 识别所属最近一层并行拆分节点实例。

## 10. 多人审批模式

### 10.1 `AND`

- 所有人都通过，节点才通过。
- 任意一人拒绝，节点立即拒绝。

### 10.2 `OR`

- 任意一人通过，节点立即通过。
- 所有人都拒绝，节点才拒绝。

### 10.3 `SEQUENTIAL`

- 按 `sort_order` 依次激活审批人。
- 任意一人拒绝，节点立即拒绝。
- 全部审批人依次通过，节点才通过。

## 11. 条件分支、并行与超时

### 11.1 条件表达式变量来源

- `tb_workflow_instance.form_data`
- `applicant_id`
- `applicant_dept_id`
- `current_time`

### 11.2 并行处理规则

- `PARALLEL_SPLIT` 负责拆分并行分支。
- `PARALLEL_JOIN` 负责聚合并行结果。
- `config_json.expect_branch_count` 记录预期分支数。

### 11.3 超时策略

- `AUTO_APPROVE`
- `AUTO_REJECT`
- `NOTIFY_ONLY`

补充规则：
- 流程定义层超时与提醒字段统一使用分钟口径。
- 前端传入 `timeoutAfterMinutes`、`remindAfterMinutes` 后，后端按分钟原值保存，不再执行小时换算。

## 12. 审批操作与通知

### 12.1 支持的审批操作

- `APPROVE`
- `REJECT`
- `DELEGATE`
- `RECALL`
- `ADD_SIGN`
- `SUBMIT`
- 超时自动处理

系统动作：
- `ROUTE`
- `SPLIT_TRIGGER`
- `JOIN_ARRIVE`
- `JOIN_PASS`
- `AUTO_APPROVE`
- `AUTO_REJECT`
- `TIMEOUT`
- `REMIND`

### 12.2 通知触发点

- 节点激活
- 节点拒绝
- 流程完成
- 催办提醒
- 超时自动处理

## 13. 状态机建议

### 13.1 流程实例状态

- `RUNNING`
- `APPROVED`
- `REJECTED`
- `CANCELED`

### 13.2 节点实例状态

- `PENDING`
- `ACTIVE`
- `PENDING_APPROVAL`
- `APPROVED`
- `REJECTED`
- `SKIPPED`
- `CANCELED`
- `TIMEOUT`

## 14. 与其他模块的衔接

- 与业务定义通过 `tb_biz_definition.workflow_definition_id` 绑定。
- 与发起权限通过 `tb_biz_definition_role_rel` 衔接。
- 与用户、角色、组织通过审批人解析规则衔接。
- 与字典、业务表单通过 `form_data` 和条件表达式衔接。

补充说明：
- 流程定义详情可直接返回 `workFlowJson`，供前端直接回显流程图。
- 若前端仍需要结构化节点、审批人、连线明细，后端可继续同时返回解析结果。

完整 SQL、流程示例和原始章节可查看 [vCoding 后台管理系统完整设计归档](./00-vCoding后台管理系统完整设计归档.md)。

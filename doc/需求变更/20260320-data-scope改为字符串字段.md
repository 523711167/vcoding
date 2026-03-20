# data_scope 改为字符串字段

## 变更原因

当前 `tb_user_role.data_scope` 使用数值型字段存储数据权限范围，不利于数据库数据直观理解，也不利于接口与设计文档统一按语义编码表达权限范围。

## 变更内容

- 将 `tb_user_role.data_scope` 从数值型字段调整为字符串字段。
- 数据权限口径统一改为使用枚举编码：`ALL`、`CUSTOM_DEPT`、`CURRENT_AND_CHILD_DEPT`、`CURRENT_DEPT`、`SELF`。
- 角色数据权限更新接口入参改为接收字符串 `dataScope`。
- 角色详情、列表、分页返回中的 `dataScope` 改为字符串字段。
- 服务层对自定义部门数据权限的判断统一使用枚举 `code`。

## 影响说明

- 数据库：新增迁移 SQL，将历史数值型 `data_scope` 转换为字符串值。
- 后端代码：调整角色实体、VO、入参校验、服务逻辑和枚举取值方式。
- 文档：更新初始化 SQL 口径和两份设计文档中的 `data_scope` 字段说明。

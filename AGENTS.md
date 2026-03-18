# Repository Guidelines

## 项目结构与模块组织
本仓库是基于 Spring Boot 3.2.5、JDK 17 的审批工作流后端项目。

项目采用前后端分离模式，当前仓库仅承载后端 API、认证鉴权、持久化与相关配置，不承载 HTML 页面、模板引擎页面或前端静态资源。

- 业务代码：`src/main/java/com/yuyu/workflow`
- 主要分层：`controller`、`service`、`service/impl`、`mapper`、`entity`、`qto`、`eto`、`vo`、`convert`、`config`、`common`
- 配置文件：`src/main/resources`
- 环境配置：`application-dev.yml`、`application-prod.yml`
- 日志配置：`src/main/resources/log/logback-spring.xml`
- 测试代码：`src/test/java`
- 初始化 SQL：`sql/`
- 设计与说明文档：`README.md`、`doc/README.md`、`doc/vCoding后台管理系统设计/`
- 构建产物：`target/`，禁止手工修改

## SQL 变更规则
- 所有新增或调整数据库结构、初始化数据、迁移脚本时，统一在 `sql/` 目录下新增或维护对应 SQL 文件。
- 新增 SQL 文件名必须使用日期格式命名，格式为 `yyyyMMdd.sql`，例如 `20260318.sql`。
- SQL 文件按周归档维护，同一周内的 SQL 变更应尽量收敛到同一个日期文件中，不要为零散改动重复拆出多个文件。
- 对已有业务 SQL 文件进行修改时，必须在 SQL 文件内补充本次修改原因说明。
- 对新增 SQL 文件，也必须在文件内补充新增原因、适用范围或变更背景说明。

## 构建、测试与开发命令
在仓库根目录使用 Maven。当前终端已配置好 JDK 17 环境变量，默认可直接执行 `mvn` 命令；若本机环境后续变更，以当前 shell 中实际生效的 `JAVA_HOME` 为准。

- `mvn clean package`：基于当前已配置的 JDK 17 构建可执行包并运行测试。
- `mvn test`：基于当前已配置的 JDK 17 执行测试。
- `mvn spring-boot:run`：基于当前已配置的 JDK 17 以默认 `dev` 环境启动服务。
- `mvn spring-boot:run -Dspring-boot.run.profiles=prod`：基于当前已配置的 JDK 17 以 `prod` 环境启动服务。


Swagger 地址：
- `/v3/api-docs`
- `/swagger-ui/index.html`

## 开发最高标准
### 分层职责
- 统一使用 4 空格缩进，包路径保持在 `com.yuyu.workflow` 下。
- 业务数据库表统一使用 `tb_` 前缀；实体映射、注解 SQL、初始化 SQL、迁移 SQL、设计文档必须保持一致。
- Spring Authorization Server、Spring Security OAuth2 等框架内置表允许直接使用框架官方表名与官方表结构，不强制改为 `tb_` 前缀。
- 当前仓库仅维护后端服务代码，禁止继续在本仓库内新增 HTML 页面、模板文件或前端静态资源。
- `Controller` 只负责参数接收、参数校验、调用 `Service`、返回结果，禁止编写业务逻辑。
- `Service` 负责完整业务逻辑、事务控制和业务规则校验。
- `Mapper` 只负责数据访问，禁止承载业务分支逻辑。
- `Entity` 只用于数据库持久化映射，禁止直接作为接口入参或出参。
- 所有持久化对象字段都必须补充清晰注释。
- 持久化对象中的公共字段应优先抽取到基类，例如 `id`、`createdAt`、`updatedAt`、`isDeleted`。

### 参数与返回
- 查询接口统一使用 `QTO`，通过请求参数绑定。
- 非查询接口统一使用 `ETO`，通过 JSON 请求体接收。
- 所有查询类必须继承 `BaseQueryQTO`，复用 `id`、`idList`，禁止重复定义同类基础字段。
- `Controller` 返回对象统一使用 `VO`。
- 所有接口结果统一封装为 `Resp<T>`，固定包含 `code`、`msg`、`data`。
- 列表查询返回 `Resp<List<VO>>`。
- 分页查询返回 `Resp<PageResult<VO>>`。
- 详情查询返回 `Resp<VO>`。

### 列表与分页
- 所有分页查询都必须同时提供对应的 `list` 接口。
- `list` 与 `page` 的查询条件、查询 SQL、返回字段、排序规则、数据权限口径必须完全一致。
- 两者唯一允许的差异是：`page` 额外携带分页参数，并返回分页信息。
- 同一业务模块下，`list` 与 `page` 必须复用同一套查询实现。
- 所有删除接口都必须优先支持批量删除，单个删除仅作为批量删除的一种特例处理。

### Mapper 与 StructMapper
- 所有 `Mapper` 都必须显式提供 `removeById` 与 `removeByIds` 方法。
- `removeById` 必须通过注解 SQL 执行按主键物理删除。
- `removeByIds` 必须通过注解 SQL 执行按主键集合批量物理删除。
- 关联表删除时，必须先查出关联记录主键，再调用对应 `Mapper.removeById` 删除。
- 需要批量删除时，必须优先调用对应 `Mapper.removeByIds`。
- 禁止继续使用分散的 `Wrapper.delete(...)` 写法。
- MapStruct 转换组件统一使用 `*StructMapper` 命名，禁止再使用 `*Convert` 命名。
- 对象转换逻辑应尽可能收敛在对应的 `*StructMapper` 中实现。
- `StructMapper` 使用旧对象时，必须返回新对象，禁止通过 `@MappingTarget` 或其他方式直接修改传入对象。

### 注释、文档与校验
- 所有方法必须补充清晰注释。
- 接口实现方法的注释写在接口定义上。
- 私有方法的注释写在私有方法本身上。
- 接口文档统一使用 `Swagger/OpenAPI` 生成。
- 所有对外接口、`QTO`、`VO`、统一返回对象都必须补全 Swagger 注解。
- 所有 `QTO` 必须做统一参数校验，优先使用注解校验。
- 对于不涉及数据库查询的固定枚举值、状态值、常量范围校验，必须优先在 `Controller` 入参层通过注解完成，禁止继续在 `Service` 层手写同类 `if` 校验。

### 异常与一致性
- 所有业务异常必须统一转换为标准响应。
- 禁止将异常堆栈、数据库错误或框架内部错误直接返回前端。
- 新增、修改、删除、启停、授权、审批处理等写操作都要考虑幂等性。
- 枚举字段统一存值不存显示文案，显示文案通过字典或 `VO` 扩展字段返回。

## 测试规范
- 测试代码放在 `src/test/java`。
- 使用 Spring Boot Test 与 JUnit 5。
- 测试类命名建议使用 `*Tests.java`。
- 优先补充服务层和控制层集成测试。
- 重点覆盖：查询条件、参数校验、对象转换、枚举处理、异常捕获。
- 测试环境配置放在 `src/test/resources/application.yml`。
- 变更查询逻辑、全局异常、MapStruct 转换、枚举公共方法时，必须同步补充测试。
- 所有人工接口测试或联调测试完成后，必须在仓库根目录 `local-test-output/` 下生成一份测试用例与实际返回结果记录，便于复核返回内容和进一步调整。
- 测试记录中必须显式保存本次使用的 `curl` 命令，命令应可直接复用，便于后续继续联调或回归验证。
- 测试记录中必须紧跟保存每条命令对应的原始返回结果；若接口返回 HTTP 状态码，也必须一并记录。
- `local-test-output/` 仅用于本地测试留痕，不提交到 Git。

## 提交与合并请求规范
- Git 提交信息统一使用 `type: 变更摘要` 格式，摘要必须使用中文，简短、明确、可直接说明本次变更目的。
- `type` 统一使用以下类型：
- `feat`：新功能、新需求、能力扩展。
- `fix`：缺陷修复、问题修正、线上问题处理。
- `refactor`：重构、结构调整、实现优化，但不改变对外行为。
- `docs`：设计文档、说明文档、注释规范、开发规范调整。
- `test`：测试用例补充、测试脚本调整、联调验证代码。
- `style`：纯格式整理，不涉及逻辑变更。
- `chore`：构建、依赖、配置、脚手架、工程化杂项调整。
- 若一次提交同时包含多类变更，应优先按“主要目的”选择 `type`，禁止在一次提交里混用多个 `type` 前缀。
- 提交摘要禁止写空泛内容，如“修改代码”“更新一下”“处理问题”等，必须落到具体模块或具体行为。
- 推荐模板：
- `feat: 新增菜单分页查询接口`
- `fix: 修复刷新令牌后 access token 未持久化问题`
- `refactor: 重构用户权限组装逻辑`
- `docs: 更新 OAuth2 设计与提交流程规范`
- `test: 新增 OAuth2 授权持久化测试`
- `chore: 调整 dev 环境数据库配置`
- 若提交包含 SQL 变更，摘要中应尽量体现影响模块。
- 示例：`feat: 新增菜单初始化 SQL 与管理接口`
- 若提交包含安全相关调整，摘要中应尽量体现认证或鉴权范围。
- 示例：`fix: 修复 opaque token 撤销后未立即失效问题`
- 提交 PR 时应说明影响模块、行为变更、接口变更、配置调整和 SQL 变更。

## 安全与配置说明
- 环境相关配置统一放在 `application-*.yml` 中。
- 禁止将数据库账号、密码或其他密钥硬编码到 Java 代码中。
- 登录认证统一使用“认证服务端 + 资源服务端”架构，两者可以部署在同一个 Jar 内，但职责必须清晰分离。
- 资源鉴权优先使用可内省的 `Bearer Token`；当业务要求“撤销后立即失效”时，优先采用 `opaque token + introspection` 方案，禁止默认使用无法即时失效的本地 JWT 验签方案。
- 令牌签发、刷新、撤销优先使用认证服务端标准端点，如 `/oauth2/token`、`/oauth2/revoke`、`/oauth2/introspect`；如需账号密码登录，优先通过 `/oauth2/token` 扩展自定义 grant_type，而不是新增平行登录发 token 接口。
- 认证相关配置统一使用 `workflow.security.issuer`、`workflow.security.token-expire-seconds`、`workflow.security.client-id`、`workflow.security.client-secret`，禁止将签名和客户端规则散落在业务代码中。
- `dev` 与 `prod` 在数据库、日志和运行行为上可能不同，修改配置时需分别检查两个环境。
- 若当前终端缺少 Maven，无法完成本地验证时，应在 PR 或变更说明中明确标注未验证项。

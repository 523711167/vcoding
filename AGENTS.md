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
- 设计与说明文档：`README.md`、`doc/README.md`、`doc/审批工作流设计/`
- 构建产物：`target/`，禁止手工修改

## 构建、测试与开发命令
在仓库根目录使用 Maven。执行前统一使用 JDK 17，当前本机可用路径为 `/Users/xixipeng/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home`：

- `JAVA_HOME=/Users/xixipeng/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home PATH=$JAVA_HOME/bin:$PATH mvn clean package`：使用 JDK 17 构建可执行包并运行测试。
- `JAVA_HOME=/Users/xixipeng/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home PATH=$JAVA_HOME/bin:$PATH mvn test`：使用 JDK 17 执行测试。
- `JAVA_HOME=/Users/xixipeng/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home PATH=$JAVA_HOME/bin:$PATH mvn spring-boot:run`：使用 JDK 17 以默认 `dev` 环境启动服务。
- `JAVA_HOME=/Users/xixipeng/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home PATH=$JAVA_HOME/bin:$PATH mvn spring-boot:run -Dspring-boot.run.profiles=prod`：使用 JDK 17 以 `prod` 环境启动服务。


Swagger 地址：
- `/v3/api-docs`
- `/swagger-ui/index.html`

## 开发最高标准
### 分层职责
- 统一使用 4 空格缩进，包路径保持在 `com.yuyu.workflow` 下。
- 所有数据库表统一使用 `tb_` 前缀；实体映射、注解 SQL、初始化 SQL、迁移 SQL、设计文档必须保持一致。
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
- 所有 `Mapper` 都必须显式提供 `deleteById` 与 `deleteByIds` 方法。
- `deleteById` 必须通过注解 SQL 执行按主键物理删除。
- `deleteByIds` 必须通过注解 SQL 执行按主键集合批量物理删除。
- 关联表删除时，必须先查出关联记录主键，再调用对应 `Mapper.deleteById` 删除。
- 需要批量删除时，必须优先调用对应 `Mapper.deleteByIds`。
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

## 提交与合并请求规范
- 当前工作区未提供可参考的 Git 提交历史，无法提炼仓库既有提交规范。
- 建议使用简短、明确、祈使句风格的提交信息。
- 示例：`feat: 新增角色分页查询`
- 示例：`refactor: 抽取用户查询构造逻辑`
- 提交 PR 时应说明影响模块、行为变更、接口变更、配置调整和 SQL 变更。
- 如果接口文档或返回结构变化，应附上 Swagger 截图或示例。

## 安全与配置说明
- 环境相关配置统一放在 `application-*.yml` 中。
- 禁止将数据库账号、密码或其他密钥硬编码到 Java 代码中。
- 登录认证统一使用“认证服务端 + 资源服务端”架构，两者可以部署在同一个 Jar 内，但职责必须清晰分离。
- 资源鉴权优先使用可内省的 `Bearer Token`；当业务要求“撤销后立即失效”时，优先采用 `opaque token + introspection` 方案，禁止默认使用无法即时失效的本地 JWT 验签方案。
- 令牌签发、刷新、撤销优先使用认证服务端标准端点，如 `/oauth2/token`、`/oauth2/revoke`、`/oauth2/introspect`；如需账号密码登录，优先通过 `/oauth2/token` 扩展自定义 grant_type，而不是新增平行登录发 token 接口。
- 认证相关配置统一使用 `workflow.security.issuer`、`workflow.security.token-expire-seconds`、`workflow.security.client-id`、`workflow.security.client-secret`，禁止将签名和客户端规则散落在业务代码中。
- `dev` 与 `prod` 在数据库、日志和运行行为上可能不同，修改配置时需分别检查两个环境。
- 若当前终端缺少 Maven，无法完成本地验证时，应在 PR 或变更说明中明确标注未验证项。

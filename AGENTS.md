# Repository Guidelines

## 项目概况
- 技术栈：Spring Boot 3.2.5、JDK 17、Maven。
- 当前仓库只承载后端 API、认证鉴权、持久化与配置，不新增 HTML、模板或前端静态资源。
- 主要代码目录：`src/main/java/com/yuyu/workflow`
- 测试目录：`src/test/java`
- 配置目录：`src/main/resources`
- SQL 目录：`sql/`
- 设计文档目录：`doc/`、`doc/vCoding后台管理系统设计/`、`doc/需求变更/`

## 常用命令
- `mvn clean package`：构建并执行测试
- `mvn test`：执行测试
- `mvn spring-boot:run`：以默认 `dev` 环境启动
- `mvn spring-boot:run -Dspring-boot.run.profiles=prod`：以 `prod` 环境启动
- Swagger：
  - `/v3/api-docs`
  - `/swagger-ui/index.html`

## 开发约束

### 联调契约
- 默认遵循“保持契约，不做兼容”原则：若前后端接口地址或字段不一致，先推动调用方按最新契约对接，不在后端临时增加旧接口兼容，除非用户明确要求做兼容方案。

### 分层
- `Controller`：只做参数接收、校验、调用服务、返回结果，不写业务逻辑。
- `Service`：负责业务逻辑、事务和规则校验。
- `Mapper`：只做数据访问，不承载业务分支。
- `Entity`：只用于持久化映射，不直接作为接口入参或出参。

### 命名与结构
- 包路径统一使用 `com.yuyu.workflow`。
- 统一 4 空格缩进。
- 业务表统一使用 `tb_` 前缀；实体、SQL、文档命名保持一致。
- 公共持久化字段优先抽到基类，如 `id`、`createdAt`、`updatedAt`、`isDeleted`。

### 参数与返回
- 查询接口使用 `QTO`，通过请求参数绑定。
- 非查询接口使用 `ETO`，通过 JSON 请求体接收。
- 查询基础参数优先复用基类，避免重复定义。
- 返回对象统一使用 `VO`。
- 所有接口统一返回 `Resp<T>`，固定包含 `code`、`msg`、`data`。

### 列表、分页、删除
- 分页查询必须同时提供对应 `list` 接口。
- 同一模块下 `list` 与 `page` 的查询条件、排序、权限口径必须一致。
- 删除接口优先支持批量删除，单个删除视为批量删除特例。

### Mapper 与转换
- 所有 `Mapper` 显式提供 `removeById`、`removeByIds`。
- 删除关联数据时，先查关联记录主键，再调用对应 `removeById/removeByIds`。
- 禁止继续使用分散的 `Wrapper.delete(...)`。
- MapStruct 组件统一命名为 `*StructMapper`，不再使用 `*Convert`。
- 更新转换返回新对象，禁止通过 `@MappingTarget` 直接修改传入对象。

### 校验、注释、异常
- 对外接口、`QTO`、`VO`、统一返回对象补全 Swagger 注解。
- 固定枚举、状态、常量范围校验优先在入参层通过注解完成。
- 所有方法补清晰注释：接口方法注释写在接口定义上，私有方法注释写在方法本身。
- 业务异常必须统一转标准响应。
- 禁止把异常堆栈、SQL 错误或框架内部错误直接返回前端。
- 写操作需考虑幂等性。

## SQL 规则
- 所有结构、初始化、迁移变更统一放在 `sql/`。
- 新增 SQL 文件名使用 `yyyyMMdd.sql`，同周变更尽量收敛到同一文件。
- 新增或修改 SQL 文件时，必须写明本次变更原因。
- init.sql记录当前的表的DDL，字段变化和新增表单需要同步更新，用户和角色和组织DML已经固定，后续菜单新增，需要更新数据。

## 测试规则
- 使用 Spring Boot Test 与 JUnit 5。
- 测试类建议命名为 `*Tests.java`。
- 变更查询逻辑、异常处理、MapStruct 转换、枚举方法时，必须补测试。
- 人工接口测试或联调后，在 `local-test-output/` 下保留测试记录。
- 测试记录必须包含：
  - `curl` 命令
  - 原始返回结果
  - 如有 HTTP 状态码，一并保留
- `local-test-output/` 仅本地留痕，不提交 Git。

## 文档规则
- 原设计文档默认视为基线。
- 新需求、设计调整、范围变更，优先写入 `doc/需求变更/`。
- 若用户明确要求直接修改设计文档，再按要求处理。

## 提交规范
- 提交信息格式：`type: 变更摘要`
- Git 提交摘要必须使用中文，简短且明确表达本次变更目的。
- 允许类型：
  - `feat`
  - `fix`
  - `refactor`
  - `docs`
  - `test`
  - `style`
  - `chore`
- 摘要必须具体，禁止使用“修改代码”“处理问题”这类空泛描述。

## 安全与配置
- 环境配置统一放在 `application-*.yml`。
- 禁止在 Java 代码中硬编码数据库账号、密码或其他密钥。
- 登录认证采用“认证服务端 + 资源服务端”架构。
- 资源鉴权优先使用可内省的 Bearer Token；需要撤销立即失效时优先 `opaque token + introspection`。
- 认证相关配置统一使用：
  - `workflow.security.issuer`
  - `workflow.security.token-expire-seconds`
  - `workflow.security.client-id`
  - `workflow.security.client-secret`

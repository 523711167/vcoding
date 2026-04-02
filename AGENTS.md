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

### 总原则
- 默认遵循“保持契约，不做兼容”原则：若前后端接口地址或字段不一致，先推动调用方按最新契约对接，不在后端临时增加旧接口兼容，除非用户明确要求做兼容方案。

### 分层职责
- `Controller`：只做参数接收、校验、调用服务、返回结果，不写业务逻辑。
- `Service`：负责业务逻辑、事务和规则校验。
- `Mapper`：只做数据访问，不承载业务分支。
- `Entity`：只用于持久化映射，不直接作为接口入参或出参。
- 引入 MyBatis-Plus 后，业务模块统一通过 `Service` 调用数据能力；除 `ServiceImpl` 内部外，禁止在 `Controller`、其他 `Service`、`Struct` 中直接注入或调用 `Mapper`。

### 命名与结构
- 包路径统一使用 `com.yuyu.workflow`。
- 统一 4 空格缩进。
- 业务表统一使用 `tb_` 前缀；实体、SQL、文档命名保持一致。
- 公共持久化字段优先抽到基类，如 `id`、`createdAt`、`updatedAt`、`isDeleted`。

### 参数与返回约定
- 查询接口使用 `QTO`，通过请求参数绑定。
- 非查询接口使用 `ETO`，通过 JSON 请求体接收。
- 查询基QTO对象只有详情、分页、列表查询的时候复用。
- 返回对象统一使用 `VO`。
- 所有接口统一返回 `Resp<T>`，固定包含 `code`、`msg`、`data`。

### 查询、列表与分页
- 分页查询统一使用 MyBatis-Plus 分页对象（`IPage`/`Page`）。
- Mapper 分页查询方法参数顺序固定：第一个参数为分页对象，第二个参数为其余查询参数对象（如 `QTO`）。
- 列表查询与分页查询保持同一套查询条件对象；相较列表查询，分页查询仅额外增加第一个分页对象参数。
- 分页查询必须同时提供对应 `list` 接口。
- 同一模块下 `list` 与 `page` 的查询条件、排序、权限口径必须一致。
- 同一模块下 `detail` 与 `list/page` 的权限口径必须一致，禁止出现详情越权或详情误拦截。

### 权限查询规则
- 涉及 `ALL`、`SELF`、`CURRENT_DEPT`、`CURRENT_AND_CHILD_DEPT`、`CUSTOM_DEPT` 组合时，必须在设计或注释中明确组合口径后再实现。
- `SELF` 必须显式限制为“仅本人可见”，禁止依赖隐式行为。
- `CUSTOM_DEPT` 与组织范围组合时，默认按交集收敛；如需特例必须在需求文档明确。
- `ALL` 分支使用动态 SQL 直接分支处理（跳过权限条件），禁止通过 `OR #{flag}=TRUE` 形式兜底。
- 需求讨论中的“示例”默认不等于“唯一规则”，实现前必须确认是否为通用规则。

### 查询安全
- 权限计算中间字段（如 `hasAllData`、`visibleDeptIdList`）仅允许服务端写入，禁止信任前端透传值。


### 删除与 Mapper 规范
- 删除接口优先支持批量删除，单个删除视为批量删除特例。
- 所有 `Mapper` 显式提供 `removeById`、`removeByIds`。
- 删除操作，业务数据使用逻辑删除，用户关联角色表 用户关联组织表 角色关联菜单表 工作流节点关联组织展开关系表 用户关联组织展开关系表都必须使用物理删除。
- 禁止继续使用分散的 `Wrapper.delete(...)`。
- 所有的数据库entity必须添加注释

### 编码规范
- 新增物理表，生成对应代码必须要生成 service mapper struct entity ，controller是可选择
- MapStruct 组件统一命名为 `*StructMapper`。
- 使用struct的，禁止通过 `@MappingTarget` 直接修改传入对象。

### mapstruct的使用场景
- 把一层的数据对象转成另一层对象，避免手写大量重复的 set/get
- 枚举值、状态值、嵌套字段的整理转换,比如把数据库状态码转成更适合返回层表达的字段
- 分页、列表结果中的对象转换,比如把 List<Entity> 批量转成 List<VO>
- 服务层内部需要不同结构对象时做转换,比如Entity -> DTO/内部对象
- 新增、修改接口收到入参后，转成持久化对象,

### 校验、注释与异常
- 对外接口、`QTO`、`VO`、统一返回对象补全 Swagger 注解。
- 固定枚举、状态、常量范围校验优先在入参层通过注解完成。
- 所有方法补清晰注释：接口方法注释写在接口定义上，私有方法注释写在方法本身。
- 业务异常必须统一转标准响应。
- 禁止把异常堆栈、SQL 错误或框架内部错误直接返回前端。
- 权限口径变更时必须补对应单测，至少覆盖 `ALL`、`SELF`、以及组合权限场景。

## SQL记录规则
- 所有结构、初始化、迁移变更统一放在 `sql/`目录下。
- 数据库结构发生变化后，DML和DDL都保存在文件名`sql/yyyyMMdd.sql`，必须写明本次变更原因，同周变更尽量收敛到同一文件。
- init.sql记录当前的数据库表的DDL，当字段发生变化都需要及时更新，用户和角色和组织关系的DML已经固定，后续菜单新增，需要同步更新DML语句。

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

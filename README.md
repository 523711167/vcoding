# approval-workflow

Spring Boot backend service for the approval workflow system.

This repository follows a front-end/back-end separation architecture and only contains backend APIs, security, persistence, and related configuration. It does not carry HTML pages, template-engine views, or front-end static site assets.

## Stack

- JDK `17`
- Spring Boot `3.2.5`
- MyBatis-Plus
- MySQL `8`

## Run

```bash
mvn spring-boot:run
```

## Swagger

- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`

## Profiles

- Default profile: `dev`
- Run with prod: `mvn spring-boot:run -Dspring-boot.run.profiles=prod`
- Or use env var: `SPRING_PROFILES_ACTIVE=prod`

## Logging

- Logback config: `src/main/resources/log/logback-spring.xml`
- `dev`: console logging
- `prod`: console + rolling file logging
- Log directory can be overridden with `LOG_HOME`

## MyBatis-Plus

- Config style: JavaBean configuration
- Config class: `src/main/java/com/yuyu/workflow/config/MybatisPlusConfig.java`
- Includes camel-case mapping, default id strategy, pagination interceptor
- SQL stdout logging is enabled only in `dev`

## ObjectMapper

- Config style: JavaBean configuration
- Config class: `src/main/java/com/yuyu/workflow/config/JacksonConfig.java`
- Conversion helper: `src/main/java/com/yuyu/workflow/common/util/ObjectMapperUtils.java`
- Used for JSON serialization and JSON deserialization
- Unified date format: `yyyy-MM-dd HH:mm:ss`

## MapStruct

- Used for object-to-object conversion such as `QTO -> Entity -> VO`
- Global config: `src/main/java/com/yuyu/workflow/config/MapStructConfig.java`
- Base mapper: `src/main/java/com/yuyu/workflow/common/mapstruct/BaseMapper.java`
- Generated mapper beans are managed by Spring
- Maven has been configured with MapStruct annotation processor

## Package

Base package: `com.yuyu.workflow`

## Docs

- 文档入口：`doc/README.md`
- 审批工作流设计拆分目录：`doc/审批工作流设计/`

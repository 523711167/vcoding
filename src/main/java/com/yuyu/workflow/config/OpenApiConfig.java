package com.yuyu.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * 注册 OpenAPI 基础信息。
     */
    @Bean
    public OpenAPI approvalWorkflowOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Token")
                                .description("Bearer Token 认证，请在请求头中传入 Authorization: Bearer {token}")))
                .info(new Info()
                        .title("Approval Workflow API")
                        .description("审批工作流系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("yuyu")));
    }
}

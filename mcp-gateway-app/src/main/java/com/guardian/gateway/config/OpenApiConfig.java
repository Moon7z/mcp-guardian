package com.guardian.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI guardianOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MCP Guardian API")
                        .description("企业级 MCP 安全代理网关 API 文档。MCP Guardian 提供安全认证、DLP 数据脱敏、策略引擎和审计日志功能。")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MCP Guardian Team"))
                        .license(new License()
                                .name("MIT License")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 认证令牌。通过 Authorization: Bearer <token> 传递。")));
    }
}

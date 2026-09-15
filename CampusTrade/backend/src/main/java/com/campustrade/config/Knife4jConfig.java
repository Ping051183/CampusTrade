package com.campustrade.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j/Swagger接口文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园二手交易平台 API文档")
                        .description("CampusTrade - 校园二手交易平台接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CampusTrade")
                                .email("support@campustrade.com")))
                // 添加JWT认证支持
                .addSecurityItem(new SecurityRequirement().addList("BearerToken"))
                .components(new Components()
                        .addSecuritySchemes("BearerToken", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("Bearer")
                                .bearerFormat("JWT")
                                .description("请输入JWT Token")));
    }
}

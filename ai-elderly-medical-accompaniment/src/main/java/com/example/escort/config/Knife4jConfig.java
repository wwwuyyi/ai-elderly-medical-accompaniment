package com.example.escort.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j接口文档配置（基于springdoc-openapi）
 * 访问地址：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI escortOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI赋能陪诊助医服务系统 API")
                        .description("面向老年群体AI赋能陪诊助医服务系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("毕设开发")
                                .email("student@example.com")));
    }
}
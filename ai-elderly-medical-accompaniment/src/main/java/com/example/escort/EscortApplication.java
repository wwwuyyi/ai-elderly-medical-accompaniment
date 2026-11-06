package com.example.escort;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AI赋能陪诊助医服务系统 启动入口
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.example.escort.config")
public class EscortApplication {

    public static void main(String[] args) {
        SpringApplication.run(EscortApplication.class, args);
    }
}
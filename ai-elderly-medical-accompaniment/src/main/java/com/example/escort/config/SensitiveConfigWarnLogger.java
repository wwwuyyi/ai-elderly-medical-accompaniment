package com.example.escort.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 敏感配置默认值告警
 * 启动完成后检查 JWT 密钥、AES 密钥、数据库密码是否仍为演示默认值，
 * 仅输出日志提醒，不阻断启动，便于评审与生产部署时自查。
 */
@Slf4j
@Component
public class SensitiveConfigWarnLogger {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${encrypt.aes-key:}")
    private String aesKey;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void warnDefaultSecrets() {
        if (jwtSecret.contains("please-change-in-production")) {
            log.warn("【安全警告】JWT 密钥使用默认值，生产环境请设置环境变量 JWT_SECRET");
        }
        if (aesKey.contains("demo")) {
            log.warn("【安全警告】AES 密钥使用默认值，生产环境请设置环境变量 AES_KEY");
        }
        if ("root".equals(dbPassword)) {
            log.warn("【安全警告】数据库密码使用默认值 root，生产环境请设置环境变量 DB_PASSWORD");
        }
    }
}
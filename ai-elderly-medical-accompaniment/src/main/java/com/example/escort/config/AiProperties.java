package com.example.escort.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI服务配置属性（全部走application.yml，禁止硬编码）
 *
 * 注：具体厂商地址与参数使用占位符 ${AI_OCR_BASE_URL} / ${AI_LLM_BASE_URL} / ${AI_API_KEY}，
 * 以主流公开API为例，具体实现需参考对应官方文档。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** OCR服务配置 */
    private OcrConfig ocr = new OcrConfig();

    /** LLM服务配置 */
    private LlmConfig llm = new LlmConfig();

    /** 连接超时（毫秒） */
    private int connectTimeoutMs = 3000;

    /** 读取超时（毫秒） */
    private int readTimeoutMs = 30000;

    /** 最大重试次数 */
    private int maxRetry = 3;

    /** 重试初始退避（毫秒），指数倍乘 */
    private int retryBackoffMs = 1000;

    /** 熔断阈值：连续失败次数 */
    private int circuitFailureThreshold = 5;

    /** 熔断半开试探间隔（毫秒） */
    private int circuitHalfOpenMs = 60000;

    @Data
    public static class OcrConfig {
        private String baseUrl;
        private String apiKey;
        private int readTimeoutMs = 15000;
    }

    @Data
    public static class LlmConfig {
        private String baseUrl;
        private String apiKey;
        private int readTimeoutMs = 30000;
    }
}
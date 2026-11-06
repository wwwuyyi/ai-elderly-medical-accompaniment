package com.example.escort.config;

import com.example.escort.common.SimpleCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiBreakerConfig {
    private final AiProperties props;
    public AiBreakerConfig(AiProperties props) { this.props = props; }

    @Bean
    public SimpleCircuitBreaker ocrBreaker() {
        return new SimpleCircuitBreaker("ocr", props.getCircuitFailureThreshold(), props.getCircuitHalfOpenMs());
    }
    @Bean
    public SimpleCircuitBreaker llmBreaker() {
        return new SimpleCircuitBreaker("llm", props.getCircuitFailureThreshold(), props.getCircuitHalfOpenMs());
    }
}
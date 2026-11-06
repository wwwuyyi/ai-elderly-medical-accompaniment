package com.example.escort.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义线程池配置（用于AI异步任务）
 *
 * 参数选择理由：
 * - 核心线程数4 + 最大线程数8：匹配AI调用（IO密集型）的并发需求，避免过度占用CPU。
 * - 队列容量50：缓冲突发请求，避免频繁触发拒绝策略。
 * - 拒绝策略CallerRunsPolicy：队列满时由调用线程执行，提供天然背压，防止任务丢失。
 * - 禁止使用 @Async 默认线程池（SimpleAsyncTaskExecutor 无界创建线程，有OOM风险）。
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
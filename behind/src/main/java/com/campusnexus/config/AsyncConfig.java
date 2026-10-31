package com.campusnexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String VECTOR_TASK_EXECUTOR = "vectorTaskExecutor";
    public static final String IMPORT_TASK_EXECUTOR = "importTaskExecutor";
    public static final String CHAT_TASK_EXECUTOR = "chatTaskExecutor";
    public static final String SSE_SCHEDULER = "sseScheduler";
    public static final String FILE_CLEANUP_EXECUTOR = "fileCleanupExecutor";

    @Bean(name = VECTOR_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor vectorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("vector-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }

    @Bean(name = IMPORT_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor importTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("import-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }

    @Bean(name = CHAT_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor chatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("chat-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean(name = FILE_CLEANUP_EXECUTOR)
    public ThreadPoolTaskExecutor fileCleanupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("file-cleanup-");
        return executor;
    }

    @Bean(name = SSE_SCHEDULER, destroyMethod = "shutdown")
    public ScheduledExecutorService sseScheduler() {
        return Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "sse-keepalive");
            thread.setDaemon(true);
            return thread;
        });
    }
}

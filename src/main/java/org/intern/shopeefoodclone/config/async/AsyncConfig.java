package org.intern.shopeefoodclone.config.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {


    /**
     * Executor specialized for File Uploading (@Async("fileUploadExecutor")).
     * Why these settings?
     * - File uploads are memory-intensive and heavy on disk/network I/O (buffering file streams).
     * - We keep moderate concurrency (core=5, max=15) and a restricted queue (capacity=50)
     *   to prevent Out-Of-Memory (OOM) errors from queuing/processing too many large files at once.
     * - CallerRunsPolicy ensures that if the queue is full, the calling thread handles the upload,
     *   effectively throttling incoming requests and protecting application stability.
     */
    @Bean(name = "fileUploadExecutor")
    public Executor fileUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("file-upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // Allow more time for large files to finish uploading during shutdown
        executor.initialize();
        log.info("fileUploadExecutor configured: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }

    /**
     * Executor specialized for Email & OTP Notifications (@Async("emailExecutor")).
     *
     * Why these settings?
     * - Sending emails is a lightweight, network-bound operation (SMTP/HTTP APIs) with very low memory footprint.
     * - Threads spend 99% of their time waiting for network socket responses from mail servers.
     * - We configure higher concurrency (core=10, max=50) and a large queue (capacity=500)
     *   to quickly absorb registration bursts and notification spikes without blocking main user threads.
     */
    @Primary
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("email-notif-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("emailExecutor configured: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}

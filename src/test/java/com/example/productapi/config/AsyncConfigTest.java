package com.example.productapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    @Test
    void executorIsConfiguredAndExecutesTasks() throws Exception {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.applicationTaskExecutor();
        assertNotNull(executor);
        assertTrue(executor.getCorePoolSize() > 0);
        assertTrue(executor.getMaxPoolSize() >= executor.getCorePoolSize());
        var task = new FutureTask<>(() -> Thread.currentThread().getName());
        executor.execute(task);
        String threadName = task.get(2, TimeUnit.SECONDS);
        assertTrue(threadName.startsWith("product-api-"), "Expected product-api thread but got: " + threadName);
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_returnsExecutor() {
        AsyncConfig config = new AsyncConfig();
        assertNotNull(config.getAsyncExecutor());
    }
}
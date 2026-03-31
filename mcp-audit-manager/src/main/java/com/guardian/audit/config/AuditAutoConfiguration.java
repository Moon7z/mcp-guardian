package com.guardian.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@ComponentScan(basePackages = "com.guardian.audit")
public class AuditAutoConfiguration {

    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        // For Java 21+, replace with: Executors.newVirtualThreadPerTaskExecutor()
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        executor.initialize();
        return executor;
    }
}

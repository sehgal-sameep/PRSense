package com.codewithsam.prsense.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
@Slf4j
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "reviewExecutor")
    public Executor reviewExecutor(ReviewProperties reviewProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(reviewProperties.getThreadPoolSize());
        executor.setMaxPoolSize(reviewProperties.getThreadPoolSize() * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("pr-sense-");
        executor.initialize();
        log.info("Review executor initialized — {} core threads", reviewProperties.getThreadPoolSize());
        return executor;
    }
}

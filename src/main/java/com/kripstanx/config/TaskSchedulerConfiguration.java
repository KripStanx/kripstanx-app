package com.kripstanx.config;

import tech.jhipster.config.JHipsterProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfiguration {
    private final JHipsterProperties jHipsterProperties;

    public TaskSchedulerConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(jHipsterProperties.getAsync().getCorePoolSize());
        threadPoolTaskScheduler.setThreadNamePrefix("krips-task-Executor-");
        return threadPoolTaskScheduler;
    }
}

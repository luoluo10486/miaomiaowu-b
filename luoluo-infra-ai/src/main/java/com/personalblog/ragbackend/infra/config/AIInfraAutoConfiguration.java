package com.personalblog.ragbackend.infra.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI基础设施自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(AIModelProperties.class)
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.personalblog.ragbackend.infra")
public class AIInfraAutoConfiguration {
}

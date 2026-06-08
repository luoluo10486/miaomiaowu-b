package com.personalblog.ragbackend.common.satoken.config;

import cn.dev33.satoken.dao.SaTokenDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalblog.ragbackend.common.satoken.core.dao.RedisSaTokenDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Sa令牌存储配置
 */
@Configuration
public class SaTokenStorageConfig {

    @Bean
    public SaTokenDao saTokenDao(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisSaTokenDao(redisTemplate, objectMapper);
    }
}

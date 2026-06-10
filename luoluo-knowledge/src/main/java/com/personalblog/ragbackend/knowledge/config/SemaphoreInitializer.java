package com.personalblog.ragbackend.knowledge.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * Semaphore初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemaphoreInitializer {

    private final RedissonClient redissonClient;
    private final RagSemaphoreProperties semaphoreProperties;

    @PostConstruct
    public void documentUploadSemaphoreInitialize() {
        initialize("document upload", semaphoreProperties.getDocumentUpload());
        initialize("qq chat import", semaphoreProperties.getChatImportQq());
        initialize("wechat chat import", semaphoreProperties.getChatImportWechat());
    }

    private void initialize(String label, RagSemaphoreProperties.PermitExpirableConfig config) {
        if (config == null) {
            return;
        }
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(config.getName());
        semaphore.setPermits(config.getMaxConcurrent());
        log.info("Initialized {} semaphore: name={}, maxConcurrent={}",
                label,
                config.getName(),
                config.getMaxConcurrent());
    }
}

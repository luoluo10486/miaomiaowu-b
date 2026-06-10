package com.personalblog.ragbackend.knowledge.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 知识文档处理降载配置
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "rag.knowledge.processing")
public class RagKnowledgeProcessingProperties {

    /**
     * 普通文档向量化批次大小
     */
    @Min(1)
    private Integer embeddingBatchSize = 4;

    /**
     * 聊天记录向量化批次大小
     */
    @Min(1)
    private Integer chatEmbeddingBatchSize = 2;

    /**
     * pgvector 写入批次大小
     */
    @Min(1)
    private Integer vectorWriteBatchSize = 5;

    /**
     * 聊天记录解析缓存最大条目数
     */
    @Min(1)
    private Integer chatTranscriptCacheMaxEntries = 1;

    /**
     * 聊天记录解析缓存存活秒数
     */
    @Min(60)
    private Integer chatTranscriptCacheTtlSeconds = 300;
}

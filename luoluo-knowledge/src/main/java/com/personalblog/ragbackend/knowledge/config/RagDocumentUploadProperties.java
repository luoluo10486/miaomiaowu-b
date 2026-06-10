package com.personalblog.ragbackend.knowledge.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 文档上传与导入安全阈值配置
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "rag.upload")
public class RagDocumentUploadProperties {

    /**
     * 普通文档上传大小上限，单位 MB
     */
    @Min(1)
    private Integer maxDocumentUploadSizeMb = 20;

    /**
     * 远程 URL 导入下载大小上限，单位 MB
     */
    @Min(1)
    private Integer maxRemoteDownloadSizeMb = 20;

    /**
     * 管道模式上传大小上限，单位 MB
     */
    @Min(1)
    private Integer maxPipelineUploadSizeMb = 10;

    /**
     * 聊天导入文件大小上限，单位 MB
     */
    @Min(1)
    private Integer maxChatImportSizeMb = 10;

    /**
     * 文本解析最大字符数，超过后直接拒绝
     */
    @Min(1)
    private Integer maxParseTextChars = 2_000_000;

    /**
     * 临时目录最少可用空间，单位 MB
     */
    @Min(1)
    private Integer minTempFreeSpaceMb = 256;
}

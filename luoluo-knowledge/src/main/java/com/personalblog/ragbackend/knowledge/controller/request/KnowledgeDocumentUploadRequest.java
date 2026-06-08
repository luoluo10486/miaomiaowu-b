package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

/**
 * 知识文档上传请求对象
 */
@Data
public class KnowledgeDocumentUploadRequest {
    private String sourceType;
    private String sourceLocation;
    private Boolean scheduleEnabled;
    private String scheduleCron;
    private String processMode;
    private String chunkStrategy;
    private String chunkConfig;
    private String pipelineId;
}

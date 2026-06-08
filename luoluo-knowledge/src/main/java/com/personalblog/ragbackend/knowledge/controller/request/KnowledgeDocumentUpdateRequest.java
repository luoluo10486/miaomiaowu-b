package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

/**
 * 知识文档更新请求对象
 */
@Data
public class KnowledgeDocumentUpdateRequest {
    private String docName;
    private String processMode;
    private String chunkStrategy;
    private String chunkConfig;
    private String pipelineId;
    private String sourceLocation;
    private Boolean scheduleEnabled;
    private String scheduleCron;
}

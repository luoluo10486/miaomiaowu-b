package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

/**
 * 知识Base创建请求对象
 */
@Data
public class KnowledgeBaseCreateRequest {
    private String name;
    private String description;
    private String embeddingModel;
    private String collectionName;
    private String visibility;
    private String allowedRoles;
}

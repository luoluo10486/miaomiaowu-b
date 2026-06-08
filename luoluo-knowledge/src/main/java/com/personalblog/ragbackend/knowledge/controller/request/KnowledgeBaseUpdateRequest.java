package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

/**
 * 知识Base更新请求对象
 */
@Data
public class KnowledgeBaseUpdateRequest {
    private String id;
    private String name;
    private String description;
    private String embeddingModel;
    private String visibility;
    private String allowedRoles;
}

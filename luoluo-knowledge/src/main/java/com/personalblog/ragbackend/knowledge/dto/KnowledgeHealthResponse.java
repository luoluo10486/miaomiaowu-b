package com.personalblog.ragbackend.knowledge.dto;

/**
 * 知识健康响应对象
 */
public record KnowledgeHealthResponse(
        boolean enabled,
        String defaultBaseCode,
        String vectorType,
        String collectionName,
        String embeddingModel,
        String chatModel
) {
}

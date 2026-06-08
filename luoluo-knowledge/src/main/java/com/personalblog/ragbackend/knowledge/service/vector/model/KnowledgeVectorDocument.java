package com.personalblog.ragbackend.knowledge.service.vector.model;

import java.util.List;
import java.util.Map;

/**
 * 知识向量文档记录类
 */
public record KnowledgeVectorDocument(
        String vectorId,
        String content,
        List<Float> embedding,
        Map<String, Object> metadata
) {
    public KnowledgeVectorDocument {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

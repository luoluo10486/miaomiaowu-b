package com.personalblog.ragbackend.knowledge.service.vector.model;

import java.util.Map;

/**
 * 向量搜索Hit记录类
 */
public record VectorSearchHit(
        String vectorId,
        double score,
        String content,
        Map<String, Object> metadata
) {
    public VectorSearchHit {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

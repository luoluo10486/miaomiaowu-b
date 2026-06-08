package com.personalblog.ragbackend.knowledge.domain;

/**
 * 知识分块记录类
 */
public record KnowledgeChunk(
        String id,
        String baseCode,
        String documentId,
        String title,
        String sourceUrl,
        int chunkIndex,
        String content,
        double score
) {
}

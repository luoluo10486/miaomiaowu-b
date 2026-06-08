package com.personalblog.ragbackend.knowledge.dto;

import java.util.List;

/**
 * 知识Citation记录类
 */
public record KnowledgeCitation(
        String documentId,
        String title,
        String sourceUrl,
        int chunkIndex,
        double score,
        String content,
        List<String> speakerSet,
        String startTime,
        String endTime,
        String bucketMonth
) {
}

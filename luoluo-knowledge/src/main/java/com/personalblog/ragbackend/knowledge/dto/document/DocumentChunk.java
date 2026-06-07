package com.personalblog.ragbackend.knowledge.dto.document;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DocumentChunk(
        int chunkIndex,
        String sectionTitle,
        String content,
        int contentLength,
        boolean overlapFromPrevious,
        Map<String, Object> metadata
) {
    public DocumentChunk {
        metadata = metadata == null || metadata.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

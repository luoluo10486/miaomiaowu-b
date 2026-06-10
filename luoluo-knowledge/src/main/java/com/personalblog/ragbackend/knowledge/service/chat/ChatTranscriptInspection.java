package com.personalblog.ragbackend.knowledge.service.chat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聊天记录轻量预扫描结果
 */
public record ChatTranscriptInspection(
        String sourceFileName,
        String platform,
        String docType,
        String groupName,
        String chatType,
        LocalDateTime exportedAt,
        Integer messageTotal,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        Map<String, Integer> monthMessageCounts
) {
    public ChatTranscriptInspection {
        monthMessageCounts = monthMessageCounts == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(monthMessageCounts));
    }
}

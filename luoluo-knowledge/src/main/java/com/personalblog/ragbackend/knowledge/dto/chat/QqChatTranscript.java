package com.personalblog.ragbackend.knowledge.dto.chat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record QqChatTranscript(
        String sourceFileName,
        String platform,
        String docType,
        String groupName,
        String chatType,
        LocalDateTime exportedAt,
        Integer messageTotal,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        List<QqChatMessage> messages
) {
    public QqChatTranscript {
        messages = messages == null ? List.of() : Collections.unmodifiableList(List.copyOf(messages));
    }
}

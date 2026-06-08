package com.personalblog.ragbackend.knowledge.dto.chat;

import java.time.LocalDateTime;

/**
 * Qq对话消息
 */
public record QqChatMessage(
        String speakerTag,
        String speakerName,
        LocalDateTime timestamp,
        String content,
        int messageIndex,
        int lineStart,
        int lineEnd
) {
}

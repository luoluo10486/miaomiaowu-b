package com.personalblog.ragbackend.knowledge.dto.chat;

import java.time.LocalDateTime;

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

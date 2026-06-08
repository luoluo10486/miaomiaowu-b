package com.personalblog.ragbackend.knowledge.service.chat;

/**
 * 对话ChunkingOptions记录类
 */
public record ChatChunkingOptions(
        int minMessages,
        int maxMessages,
        int overlapMessages,
        int targetChars,
        int maxChars,
        int splitGapMinutes,
        int maxChunkCount
) {
    public ChatChunkingOptions {
        minMessages = Math.max(1, minMessages);
        maxMessages = Math.max(minMessages, maxMessages);
        overlapMessages = Math.max(0, Math.min(overlapMessages, Math.max(0, maxMessages - 1)));
        targetChars = Math.max(128, targetChars);
        maxChars = Math.max(targetChars, maxChars);
        splitGapMinutes = Math.max(1, splitGapMinutes);
        maxChunkCount = Math.max(1, maxChunkCount);
    }
}

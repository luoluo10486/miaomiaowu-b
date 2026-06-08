package com.personalblog.ragbackend.rag.core.memory;

import com.personalblog.ragbackend.infra.convention.ChatMessage;

/**
 * 会话记忆汇总服务接口
 */
public interface ConversationMemorySummaryService {
    void compressIfNeeded(String conversationId, Long userId, ChatMessage message);

    ChatMessage loadLatestSummary(String conversationId, Long userId);

    ChatMessage decorateIfNeeded(ChatMessage summary);
}

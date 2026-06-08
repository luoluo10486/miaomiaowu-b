package com.personalblog.ragbackend.rag.core.memory;

import com.personalblog.ragbackend.infra.convention.ChatMessage;

import java.util.List;

/**
 * 会话记忆存储
 */
public interface ConversationMemoryStore {
    List<ChatMessage> loadHistory(String conversationId, Long userId);

    String append(String conversationId, Long userId, ChatMessage message);
}

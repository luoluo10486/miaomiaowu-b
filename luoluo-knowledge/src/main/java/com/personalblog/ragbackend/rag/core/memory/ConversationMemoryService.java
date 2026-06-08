package com.personalblog.ragbackend.rag.core.memory;

import com.personalblog.ragbackend.infra.convention.ChatMessage;

import java.util.List;

/**
 * 会话记忆服务接口
 */
public interface ConversationMemoryService {
    List<ChatMessage> load(String conversationId, Long userId);

    String append(String conversationId, Long userId, ChatMessage message);

    default List<ChatMessage> loadAndAppend(String conversationId, Long userId, ChatMessage message) {
        List<ChatMessage> history = load(conversationId, userId);
        append(conversationId, userId, message);
        return history;
    }
}

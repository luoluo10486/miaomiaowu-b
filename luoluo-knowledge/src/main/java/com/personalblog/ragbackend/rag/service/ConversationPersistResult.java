package com.personalblog.ragbackend.rag.service;

/**
 * 会话Persist结果记录类
 */
public record ConversationPersistResult(
        String assistantMessageId,
        String conversationTitle
) {
}

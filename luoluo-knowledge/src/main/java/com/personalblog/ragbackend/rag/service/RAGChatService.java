package com.personalblog.ragbackend.rag.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG对话服务接口
 */
public interface RAGChatService {

    void streamChat(String question, String conversationId, Boolean deepThinking, SseEmitter emitter);

    void stopTask(String taskId);
}

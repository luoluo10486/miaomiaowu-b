package com.personalblog.ragbackend.rag.service;

import com.personalblog.ragbackend.common.context.LoginUser;
import com.personalblog.ragbackend.common.web.sse.SseEmitterSender;

/**
 * 流式对话处理器Params记录类
 */
public record StreamChatHandlerParams(
        SseEmitterSender sender,
        String taskId,
        String conversationId,
        RagConversationService ragConversationService,
        LoginUser loginUser,
        String question,
        String baseCode,
        int citationCount,
        int chunkSize,
        StreamTaskManager taskManager,
        boolean userQuestionPrePersisted
) {
    public StreamChatHandlerParams(SseEmitterSender sender,
                                   String taskId,
                                   String conversationId,
                                   RagConversationService ragConversationService,
                                   LoginUser loginUser,
                                   String question,
                                   String baseCode,
                                   int citationCount,
                                   int chunkSize,
                                   StreamTaskManager taskManager) {
        this(sender, taskId, conversationId, ragConversationService, loginUser, question, baseCode,
                citationCount, chunkSize, taskManager, false);
    }
}

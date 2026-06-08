package com.personalblog.ragbackend.knowledge.dto;

import java.util.List;

/**
 * 知识Ask响应对象
 */
public record KnowledgeAskResponse(
        String answer,
        String baseCode,
        List<KnowledgeCitation> citations,
        KnowledgeTrace trace,
        String assistantMessageId,
        String conversationTitle
) {
}

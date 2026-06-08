package com.personalblog.ragbackend.knowledge.dto;

import java.util.List;

/**
 * 知识查询改写结果记录类
 */
public record KnowledgeQueryRewriteResult(
        String originalQuestion,
        String rewrittenQuestion,
        List<String> appliedMappings,
        List<String> subQuestions
) {
}

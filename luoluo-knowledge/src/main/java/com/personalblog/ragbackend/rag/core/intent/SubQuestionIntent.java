package com.personalblog.ragbackend.rag.core.intent;

import java.util.List;

/**
 * Sub问题意图记录类
 */
public record SubQuestionIntent(
        String subQuestion,
        List<NodeScore> nodeScores
) {
}

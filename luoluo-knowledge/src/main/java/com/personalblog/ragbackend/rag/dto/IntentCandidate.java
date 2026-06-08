package com.personalblog.ragbackend.rag.dto;

import com.personalblog.ragbackend.rag.core.intent.NodeScore;

/**
 * 意图Candidate记录类
 */
public record IntentCandidate(int subQuestionIndex, NodeScore nodeScore) {
}

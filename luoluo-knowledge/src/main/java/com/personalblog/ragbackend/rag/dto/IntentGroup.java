package com.personalblog.ragbackend.rag.dto;

import com.personalblog.ragbackend.rag.core.intent.NodeScore;

import java.util.List;

/**
 * 意图Group记录类
 */
public record IntentGroup(List<NodeScore> mcpIntents, List<NodeScore> kbIntents) {
}

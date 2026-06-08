package com.personalblog.ragbackend.knowledge.dto.rag;

import java.util.List;

/**
 * RAG追踪详情View记录类
 */
public record RagTraceDetailView(
        RagTraceRunView run,
        List<RagTraceNodeView> nodes
) {
}

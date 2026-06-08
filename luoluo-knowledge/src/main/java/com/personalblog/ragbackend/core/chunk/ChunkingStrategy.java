package com.personalblog.ragbackend.core.chunk;

import com.personalblog.ragbackend.knowledge.dto.document.DocumentChunk;

import java.util.List;

/**
 * Chunking策略接口
 */
public interface ChunkingStrategy {

    ChunkingMode getType();

    List<DocumentChunk> chunk(String text, TextChunkingOptions options);
}

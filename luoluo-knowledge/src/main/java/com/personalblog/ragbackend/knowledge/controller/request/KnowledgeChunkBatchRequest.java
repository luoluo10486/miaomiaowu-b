package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

import java.util.List;

/**
 * 知识分块批量请求对象
 */
@Data
public class KnowledgeChunkBatchRequest {
    private List<String> chunkIds;
}

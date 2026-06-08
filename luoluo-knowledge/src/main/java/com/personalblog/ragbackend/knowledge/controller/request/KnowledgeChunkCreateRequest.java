package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

/**
 * 知识分块创建请求对象
 */
@Data
public class KnowledgeChunkCreateRequest {
    private String content;
    private Integer index;
    private String chunkId;
}

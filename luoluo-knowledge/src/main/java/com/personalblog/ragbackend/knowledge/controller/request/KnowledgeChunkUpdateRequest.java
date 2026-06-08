package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

/**
 * 知识分块更新请求对象
 */
@Data
public class KnowledgeChunkUpdateRequest {
    private String content;
}

package com.personalblog.ragbackend.knowledge.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识分块视图对象
 */
@Data
public class KnowledgeChunkVO {
    private String id;
    private String kbId;
    private String docId;
    private Integer chunkIndex;
    private String content;
    private String contentHash;
    private Integer charCount;
    private Integer tokenCount;
    private Integer enabled;
    private String metadata;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

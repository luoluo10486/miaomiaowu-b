package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

/**
 * 知识文档创建请求对象
 */
@Data
public class KnowledgeDocumentCreateRequest {
    private String kbId;
    private String docName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer enabled;
}

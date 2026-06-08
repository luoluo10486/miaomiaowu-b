package com.personalblog.ragbackend.knowledge.controller.vo;

import lombok.Data;

/**
 * 知识文档搜索视图对象
 */
@Data
public class KnowledgeDocumentSearchVO {
    private String id;
    private String kbId;
    private String docName;
    private String kbName;
}

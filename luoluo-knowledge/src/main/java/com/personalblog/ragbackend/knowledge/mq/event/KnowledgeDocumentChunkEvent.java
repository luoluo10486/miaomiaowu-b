package com.personalblog.ragbackend.knowledge.mq.event;

/**
 * 知识文档分块事件
 */
public class KnowledgeDocumentChunkEvent {
    private Long documentId;
    private String operator;

    public KnowledgeDocumentChunkEvent() {
    }

    public KnowledgeDocumentChunkEvent(Long documentId, String operator) {
        this.documentId = documentId;
        this.operator = operator;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}

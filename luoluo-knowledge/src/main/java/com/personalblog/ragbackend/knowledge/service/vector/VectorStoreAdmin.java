package com.personalblog.ragbackend.knowledge.service.vector;

/**
 * 向量存储管理端接口
 */
public interface VectorStoreAdmin {

    void ensureVectorSpace(KnowledgeVectorSpace vectorSpace);

    boolean vectorSpaceExists(KnowledgeVectorSpaceId spaceId);
}

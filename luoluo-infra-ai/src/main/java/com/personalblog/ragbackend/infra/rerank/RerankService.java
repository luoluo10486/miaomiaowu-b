package com.personalblog.ragbackend.infra.rerank;

import com.personalblog.ragbackend.infra.convention.RetrievedChunk;

import java.util.List;

/**
 * 重排序服务接口
 */
public interface RerankService {

    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN);
}

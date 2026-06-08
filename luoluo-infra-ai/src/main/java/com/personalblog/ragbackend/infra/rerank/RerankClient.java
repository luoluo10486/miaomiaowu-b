package com.personalblog.ragbackend.infra.rerank;

import com.personalblog.ragbackend.infra.convention.RetrievedChunk;
import com.personalblog.ragbackend.infra.model.ModelTarget;

import java.util.List;

/**
 * 重排序客户端接口
 */
public interface RerankClient {

    String provider();

    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN, ModelTarget target);
}

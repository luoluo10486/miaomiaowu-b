package com.personalblog.ragbackend.infra.embedding;

import com.personalblog.ragbackend.infra.model.ModelTarget;

import java.util.List;

/**
 * 向量化客户端接口
 */
public interface EmbeddingClient {

    String provider();

    List<Float> embed(String text, ModelTarget target);

    List<List<Float>> embedBatch(List<String> texts, ModelTarget target);
}

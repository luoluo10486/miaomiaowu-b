package com.personalblog.ragbackend.rag.core.retrieve.postprocessor;

import com.personalblog.ragbackend.infra.convention.RetrievedChunk;
import com.personalblog.ragbackend.rag.core.retrieve.channel.SearchChannelResult;
import com.personalblog.ragbackend.rag.core.retrieve.channel.SearchContext;

import java.util.List;

/**
 * 搜索结果后处理器
 */
public interface SearchResultPostProcessor {

    String getName();

    int getOrder();

    default boolean isEnabled(SearchContext context) {
        return true;
    }

    List<RetrievedChunk> process(List<RetrievedChunk> chunks, List<SearchChannelResult> results, SearchContext context);
}

package com.personalblog.ragbackend.ingestion.controller.request;

import com.personalblog.ragbackend.rag.core.vector.VectorSpaceId;
import com.personalblog.ragbackend.rag.controller.request.DocumentSourceRequest;
import lombok.Data;

import java.util.Map;

/**
 * Ingestion任务创建请求对象
 */
@Data
public class IngestionTaskCreateRequest {
    private String pipelineId;
    private DocumentSourceRequest source;
    private Map<String, Object> metadata;
    private VectorSpaceId vectorSpaceId;
}

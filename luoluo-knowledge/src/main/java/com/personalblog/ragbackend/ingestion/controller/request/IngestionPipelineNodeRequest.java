package com.personalblog.ragbackend.ingestion.controller.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Ingestion流程节点请求对象
 */
@Data
public class IngestionPipelineNodeRequest {
    private String nodeId;
    private String nodeType;
    private JsonNode settings;
    private JsonNode condition;
    private String nextNodeId;
}

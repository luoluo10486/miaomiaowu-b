package com.personalblog.ragbackend.ingestion.controller.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Ingestion流程节点视图对象
 */
@Data
public class IngestionPipelineNodeVO {
    private String id;
    private String nodeId;
    private String nodeType;
    private JsonNode settings;
    private JsonNode condition;
    private String nextNodeId;
}

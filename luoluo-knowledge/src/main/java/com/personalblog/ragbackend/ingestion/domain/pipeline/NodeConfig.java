package com.personalblog.ragbackend.ingestion.domain.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeConfig {
    private String nodeId;
    private String nodeType;
    private JsonNode settings;
    private JsonNode condition;
    private String nextNodeId;
}

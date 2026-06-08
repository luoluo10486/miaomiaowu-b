package com.personalblog.ragbackend.ingestion.domain.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流程Definition类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineDefinition {
    private String id;
    private String name;
    private String description;
    private List<NodeConfig> nodes;
}

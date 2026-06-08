package com.personalblog.ragbackend.ingestion.controller.request;

import lombok.Data;

import java.util.List;

/**
 * Ingestion流程更新请求对象
 */
@Data
public class IngestionPipelineUpdateRequest {
    private String name;
    private String description;
    private List<IngestionPipelineNodeRequest> nodes;
}

package com.personalblog.ragbackend.ingestion.controller.request;

import lombok.Data;

import java.util.List;

/**
 * Ingestion流程创建请求对象
 */
@Data
public class IngestionPipelineCreateRequest {
    private String name;
    private String description;
    private List<IngestionPipelineNodeRequest> nodes;
}

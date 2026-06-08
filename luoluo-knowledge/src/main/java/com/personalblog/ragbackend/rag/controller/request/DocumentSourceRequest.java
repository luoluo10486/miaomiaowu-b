package com.personalblog.ragbackend.rag.controller.request;

import com.personalblog.ragbackend.ingestion.domain.enums.SourceType;
import lombok.Data;

import java.util.Map;

/**
 * 文档来源请求对象
 */
@Data
public class DocumentSourceRequest {
    private SourceType type;
    private String location;
    private String fileName;
    private Map<String, String> credentials;
}

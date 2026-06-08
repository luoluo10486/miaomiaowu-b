package com.personalblog.ragbackend.ingestion.domain.result;

import com.personalblog.ragbackend.ingestion.domain.enums.IngestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ingestion结果类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionResult {

    private String taskId;

    private String pipelineId;

    private IngestionStatus status;

    private Integer chunkCount;

    private String message;
}

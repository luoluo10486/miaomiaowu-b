package com.personalblog.ragbackend.rag.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG追踪详情视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagTraceDetailVO {
    private RagTraceRunVO run;
    private List<RagTraceNodeVO> nodes;
}

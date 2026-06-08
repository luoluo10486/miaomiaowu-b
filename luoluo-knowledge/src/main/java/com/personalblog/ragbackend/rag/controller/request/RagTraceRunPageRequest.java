package com.personalblog.ragbackend.rag.controller.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.EqualsAndHashCode;
import lombok.Data;

/**
 * RAG追踪Run分页请求对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RagTraceRunPageRequest extends Page {
    private String traceId;
    private String conversationId;
    private String taskId;
    private String status;
}

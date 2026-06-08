package com.personalblog.ragbackend.rag.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 意图节点批量请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentNodeBatchRequest {
    private List<String> ids;
}

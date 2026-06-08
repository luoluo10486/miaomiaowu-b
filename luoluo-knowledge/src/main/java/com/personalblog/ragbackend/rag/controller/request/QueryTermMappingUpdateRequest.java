package com.personalblog.ragbackend.rag.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询Term映射更新请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryTermMappingUpdateRequest {
    private String sourceTerm;
    private String targetTerm;
    private Integer matchType;
    private Integer priority;
    private Boolean enabled;
    private String remark;
}

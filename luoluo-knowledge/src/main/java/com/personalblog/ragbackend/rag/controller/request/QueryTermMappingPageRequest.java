package com.personalblog.ragbackend.rag.controller.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.EqualsAndHashCode;
import lombok.Data;

/**
 * 查询Term映射分页请求对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryTermMappingPageRequest extends Page {
    private String keyword;
}

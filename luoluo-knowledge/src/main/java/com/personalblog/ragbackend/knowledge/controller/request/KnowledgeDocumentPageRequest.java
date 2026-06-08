package com.personalblog.ragbackend.knowledge.controller.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.EqualsAndHashCode;
import lombok.Data;

/**
 * 知识文档分页请求对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class KnowledgeDocumentPageRequest extends Page {
    private String status;
    private String keyword;
}

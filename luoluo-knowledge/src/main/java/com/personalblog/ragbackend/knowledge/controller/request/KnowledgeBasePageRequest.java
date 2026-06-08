package com.personalblog.ragbackend.knowledge.controller.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.EqualsAndHashCode;
import lombok.Data;

/**
 * 知识Base分页请求对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class KnowledgeBasePageRequest extends Page {
    private String name;
}

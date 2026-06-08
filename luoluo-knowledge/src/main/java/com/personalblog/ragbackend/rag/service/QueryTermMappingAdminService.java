package com.personalblog.ragbackend.rag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.personalblog.ragbackend.rag.controller.request.QueryTermMappingCreateRequest;
import com.personalblog.ragbackend.rag.controller.request.QueryTermMappingPageRequest;
import com.personalblog.ragbackend.rag.controller.request.QueryTermMappingUpdateRequest;
import com.personalblog.ragbackend.rag.controller.vo.QueryTermMappingVO;

/**
 * 查询Term映射管理端服务接口
 */
public interface QueryTermMappingAdminService {
    String create(QueryTermMappingCreateRequest requestParam);

    void update(String id, QueryTermMappingUpdateRequest requestParam);

    void delete(String id);

    QueryTermMappingVO queryById(String id);

    IPage<QueryTermMappingVO> pageQuery(QueryTermMappingPageRequest requestParam);
}

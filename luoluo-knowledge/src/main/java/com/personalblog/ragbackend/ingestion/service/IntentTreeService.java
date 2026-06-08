package com.personalblog.ragbackend.ingestion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personalblog.ragbackend.rag.dao.entity.IntentNodeEntity;
import com.personalblog.ragbackend.rag.controller.request.IntentNodeCreateRequest;
import com.personalblog.ragbackend.rag.controller.request.IntentNodeUpdateRequest;
import com.personalblog.ragbackend.rag.controller.vo.IntentNodeTreeVO;

import java.util.List;

/**
 * 意图树服务接口
 */
public interface IntentTreeService extends IService<IntentNodeEntity> {
    List<IntentNodeTreeVO> getFullTree();

    String createNode(IntentNodeCreateRequest requestParam);

    void updateNode(String id, IntentNodeUpdateRequest requestParam);

    void deleteNode(String id);

    void batchEnableNodes(List<String> ids);

    void batchDisableNodes(List<String> ids);

    void batchDeleteNodes(List<String> ids);

    int initFromFactory();
}


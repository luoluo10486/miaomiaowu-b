package com.personalblog.ragbackend.knowledge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personalblog.ragbackend.common.context.UserContext;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeBaseCreateRequest;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeBasePageRequest;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeBaseUpdateRequest;
import com.personalblog.ragbackend.knowledge.controller.vo.KnowledgeBaseVO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeBaseDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentDO;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentMapper;
import com.personalblog.ragbackend.knowledge.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                    KnowledgeDocumentMapper knowledgeDocumentMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
    }

    @Override
    @Transactional
    public String create(KnowledgeBaseCreateRequest requestParam) {
        String name = requireText(requestParam.getName(), "knowledge base name is required");
        String normalizedName = name.replaceAll("\\s+", "");
        long nameCount = knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseDO>()
                .eq(KnowledgeBaseDO::getName, normalizedName)
                .eq(KnowledgeBaseDO::getDeleted, 0));
        if (nameCount > 0) {
            throw new IllegalArgumentException("knowledge base name already exists");
        }
        String collectionName = resolveCollectionName(name, requestParam.getCollectionName());
        String embeddingModel = StringUtils.hasText(requestParam.getEmbeddingModel())
                ? requestParam.getEmbeddingModel().trim()
                : "Qwen/Qwen3-Embedding-8B";
        log.info("Creating knowledge base: name='{}', collection='{}', embedding='{}'", name, collectionName, embeddingModel);
        assertCollectionAvailable(collectionName, null);

        KnowledgeBaseDO entity = new KnowledgeBaseDO();
        entity.setName(name);
        entity.setEmbeddingModel(embeddingModel);
        entity.setCollectionName(collectionName);
        entity.setOwnerUserId(parseUserId(UserContext.getUserId()));
        entity.setCreatedBy(parseUserId(UserContext.getUserId()));
        entity.setUpdatedBy(parseUserId(UserContext.getUserId()));
        entity.setDeleted(0);
        try {
            knowledgeBaseMapper.insert(entity);
            log.info("Knowledge base inserted: id={}, collection='{}'", entity.getId(), collectionName);
            log.info("Knowledge base created successfully: id={}, collection='{}', embedding='{}'",
                    entity.getId(), collectionName, entity.getEmbeddingModel());
            return String.valueOf(entity.getId());
        } catch (RuntimeException exception) {
            log.error("Failed to create knowledge base: name='{}', collection='{}', embedding='{}'",
                    name, collectionName, embeddingModel, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void update(KnowledgeBaseUpdateRequest requestParam) {
        KnowledgeBaseDO entity = requireKnowledgeBase(parseId(requestParam.getId()));
        if (StringUtils.hasText(requestParam.getName())) {
            String name = requestParam.getName().trim().replaceAll("\\s+", "");
            long nameCount = knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseDO>()
                    .eq(KnowledgeBaseDO::getName, name)
                    .eq(KnowledgeBaseDO::getDeleted, 0)
                    .ne(KnowledgeBaseDO::getId, entity.getId()));
            if (nameCount > 0) {
                throw new IllegalArgumentException("knowledge base name already exists");
            }
            entity.setName(requestParam.getName().trim());
        }
        if (StringUtils.hasText(requestParam.getEmbeddingModel())) {
            String embeddingModel = requestParam.getEmbeddingModel().trim();
            if (!embeddingModel.equals(entity.getEmbeddingModel())) {
                long documentCount = knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentDO>()
                        .eq(KnowledgeDocumentDO::getKbId, entity.getId())
                        .gt(KnowledgeDocumentDO::getChunkCount, 0)
                        .eq(KnowledgeDocumentDO::getDeleted, 0));
                if (documentCount > 0) {
                    throw new IllegalArgumentException("knowledge base already has vectorized documents, embedding model cannot be changed");
                }
            }
            entity.setEmbeddingModel(embeddingModel);
        }
        entity.setUpdatedBy(parseUserId(UserContext.getUserId()));
        knowledgeBaseMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void rename(String kbId, KnowledgeBaseUpdateRequest requestParam) {
        KnowledgeBaseDO entity = requireKnowledgeBase(parseId(kbId));
        if (!StringUtils.hasText(requestParam.getName())) {
            throw new IllegalArgumentException("knowledge base name is required");
        }
        String name = requestParam.getName().trim().replaceAll("\\s+", "");
        long nameCount = knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseDO>()
                .eq(KnowledgeBaseDO::getName, name)
                .eq(KnowledgeBaseDO::getDeleted, 0)
                .ne(KnowledgeBaseDO::getId, entity.getId()));
        if (nameCount > 0) {
            throw new IllegalArgumentException("knowledge base name already exists");
        }
        entity.setName(requestParam.getName());
        entity.setUpdatedBy(parseUserId(UserContext.getUserId()));
        knowledgeBaseMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void delete(String kbId) {
        KnowledgeBaseDO entity = requireKnowledgeBase(parseId(kbId));
        long documentCount = knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentDO>()
                .eq(KnowledgeDocumentDO::getKbId, entity.getId())
                .eq(KnowledgeDocumentDO::getDeleted, 0));
        if (documentCount > 0) {
            throw new IllegalArgumentException("该知识库下还有文档，请先删除文档后再删除知识库");
        }
        knowledgeBaseMapper.deleteById(entity.getId());
    }

    @Override
    public KnowledgeBaseVO queryById(String kbId) {
        KnowledgeBaseDO entity = requireKnowledgeBase(parseId(kbId));
        KnowledgeBaseVO vo = toView(entity, knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentDO>()
                .eq(KnowledgeDocumentDO::getKbId, entity.getId())
                .eq(KnowledgeDocumentDO::getDeleted, 0)));
        return vo;
    }

    @Override
    public IPage<KnowledgeBaseVO> pageQuery(KnowledgeBasePageRequest requestParam) {
        Page<KnowledgeBaseDO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<KnowledgeBaseDO> result = knowledgeBaseMapper.selectPage(
                page,
                new LambdaQueryWrapper<KnowledgeBaseDO>()
                        .like(StringUtils.hasText(requestParam.getName()), KnowledgeBaseDO::getName, requestParam.getName())
                        .orderByDesc(KnowledgeBaseDO::getUpdatedAt)
        );
        List<Long> kbIds = result.getRecords().stream().map(KnowledgeBaseDO::getId).toList();
        Map<Long, Long> documentCountMap = kbIds.isEmpty()
                ? Map.of()
                : knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentDO>()
                        .in(KnowledgeDocumentDO::getKbId, kbIds)
                        .eq(KnowledgeDocumentDO::getDeleted, 0))
                .stream()
                .collect(Collectors.groupingBy(KnowledgeDocumentDO::getKbId, Collectors.counting()));
        return result.convert(entity -> {
            return toView(entity, documentCountMap.getOrDefault(entity.getId(), 0L));
        });
    }

    private KnowledgeBaseVO toView(KnowledgeBaseDO entity, Long documentCount) {
        KnowledgeBaseVO vo = BeanUtil.toBean(entity, KnowledgeBaseVO.class);
        vo.setId(String.valueOf(entity.getId()));
        vo.setDocumentCount(documentCount);
        vo.setCreatedBy(entity.getCreatedBy() == null ? null : String.valueOf(entity.getCreatedBy()));
        vo.setCreateTime(entity.getCreatedAt() == null ? null : java.util.Date.from(entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        vo.setUpdateTime(entity.getUpdatedAt() == null ? null : java.util.Date.from(entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        return vo;
    }

    private KnowledgeBaseDO requireKnowledgeBase(Long kbId) {
        KnowledgeBaseDO entity = knowledgeBaseMapper.selectById(kbId);
        if (entity == null) {
            throw new IllegalArgumentException("knowledge base not found");
        }
        return entity;
    }

    private Long parseId(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return Long.valueOf(value.trim());
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private void assertCollectionAvailable(String collectionName, Long currentKbId) {
        KnowledgeBaseDO existing = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBaseDO>()
                .eq(KnowledgeBaseDO::getCollectionName, collectionName)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(currentKbId)) {
            throw new IllegalArgumentException("collection name already exists");
        }
    }

    private String resolveCollectionName(String name, String requestedCollectionName) {
        if (StringUtils.hasText(requestedCollectionName)) {
            return requestedCollectionName.trim();
        }

        String normalizedName = name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        String suffix = IdUtil.getSnowflakeNextIdStr();
        if (!StringUtils.hasText(normalizedName)) {
            return "kb_" + suffix;
        }
        if (normalizedName.length() > 80) {
            normalizedName = normalizedName.substring(0, 80);
        }
        return "kb_" + normalizedName + "_" + suffix;
    }

    private Long parseUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            return Long.valueOf(userId.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}


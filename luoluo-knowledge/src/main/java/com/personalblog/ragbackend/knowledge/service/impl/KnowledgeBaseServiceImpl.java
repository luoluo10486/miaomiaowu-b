package com.personalblog.ragbackend.knowledge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personalblog.ragbackend.common.context.UserContext;
import com.personalblog.ragbackend.common.auth.RoleUtils;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeBaseCreateRequest;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeBasePageRequest;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeBaseUpdateRequest;
import com.personalblog.ragbackend.knowledge.controller.vo.KnowledgeBaseVO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeBaseDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentDO;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentMapper;
import com.personalblog.ragbackend.knowledge.service.KnowledgeBaseAccessService;
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
    private final KnowledgeBaseAccessService knowledgeBaseAccessService;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                    KnowledgeDocumentMapper knowledgeDocumentMapper,
                                    KnowledgeBaseAccessService knowledgeBaseAccessService) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeBaseAccessService = knowledgeBaseAccessService;
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
        String allowedRoles = normalizeAllowedRoles(requestParam.getAllowedRoles());
        String visibility = resolveVisibility(requestParam.getVisibility(), allowedRoles);
        validateRoleScope(visibility, allowedRoles);
        log.info("Creating knowledge base: name='{}', collection='{}', embedding='{}'", name, collectionName, embeddingModel);
        assertCollectionAvailable(collectionName, null);

        KnowledgeBaseDO entity = new KnowledgeBaseDO();
        entity.setName(name);
        entity.setDescription(blankToNull(requestParam.getDescription()));
        entity.setEmbeddingModel(embeddingModel);
        entity.setCollectionName(collectionName);
        entity.setVisibility(visibility);
        entity.setStatus("ACTIVE");
        entity.setAllowedRoles(allowedRoles);
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
        knowledgeBaseAccessService.assertManageable(entity);
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
        if (requestParam.getDescription() != null) {
            entity.setDescription(blankToNull(requestParam.getDescription()));
        }
        String allowedRoles = requestParam.getAllowedRoles() == null ? entity.getAllowedRoles() : normalizeAllowedRoles(requestParam.getAllowedRoles());
        String visibility = resolveVisibility(requestParam.getVisibility() == null ? entity.getVisibility() : requestParam.getVisibility(), allowedRoles);
        validateRoleScope(visibility, allowedRoles);
        entity.setVisibility(visibility);
        entity.setAllowedRoles(allowedRoles);
        entity.setUpdatedBy(parseUserId(UserContext.getUserId()));
        knowledgeBaseMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void rename(String kbId, KnowledgeBaseUpdateRequest requestParam) {
        KnowledgeBaseDO entity = requireKnowledgeBase(parseId(kbId));
        knowledgeBaseAccessService.assertManageable(entity);
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
        knowledgeBaseAccessService.assertManageable(entity);
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
        knowledgeBaseAccessService.assertReadable(entity);
        KnowledgeBaseVO vo = toView(entity, knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentDO>()
                .eq(KnowledgeDocumentDO::getKbId, entity.getId())
                .eq(KnowledgeDocumentDO::getDeleted, 0)));
        return vo;
    }

    @Override
    public IPage<KnowledgeBaseVO> pageQuery(KnowledgeBasePageRequest requestParam) {
        List<KnowledgeBaseDO> visibleKnowledgeBases = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseDO>()
                        .eq(KnowledgeBaseDO::getDeleted, 0)
                        .like(StringUtils.hasText(requestParam.getName()), KnowledgeBaseDO::getName, requestParam.getName())
                        .orderByDesc(KnowledgeBaseDO::getUpdatedAt)
        ).stream()
                .filter(knowledgeBaseAccessService::canRead)
                .toList();

        long current = requestParam.getCurrent() <= 0 ? 1 : requestParam.getCurrent();
        long size = requestParam.getSize() <= 0 ? 10 : requestParam.getSize();
        int fromIndex = (int) Math.min((current - 1) * size, visibleKnowledgeBases.size());
        int toIndex = (int) Math.min(fromIndex + size, visibleKnowledgeBases.size());
        List<KnowledgeBaseDO> pageRecords = fromIndex >= toIndex ? List.of() : visibleKnowledgeBases.subList(fromIndex, toIndex);

        List<Long> kbIds = pageRecords.stream().map(KnowledgeBaseDO::getId).toList();
        Map<Long, Long> documentCountMap = kbIds.isEmpty()
                ? Map.of()
                : knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentDO>()
                        .in(KnowledgeDocumentDO::getKbId, kbIds)
                        .eq(KnowledgeDocumentDO::getDeleted, 0))
                .stream()
                .collect(Collectors.groupingBy(KnowledgeDocumentDO::getKbId, Collectors.counting()));
        Page<KnowledgeBaseVO> voPage = new Page<>(current, size, visibleKnowledgeBases.size());
        voPage.setRecords(pageRecords.stream()
                .map(entity -> toView(entity, documentCountMap.getOrDefault(entity.getId(), 0L)))
                .toList());
        return voPage;
    }

    private KnowledgeBaseVO toView(KnowledgeBaseDO entity, Long documentCount) {
        KnowledgeBaseVO vo = BeanUtil.toBean(entity, KnowledgeBaseVO.class);
        vo.setId(String.valueOf(entity.getId()));
        vo.setOwnerUserId(entity.getOwnerUserId() == null ? null : String.valueOf(entity.getOwnerUserId()));
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

    private String normalizeVisibility(String visibility) {
        if (!StringUtils.hasText(visibility)) {
            return "PRIVATE";
        }
        String normalized = visibility.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PUBLIC", "PRIVATE", "ROLES" -> normalized;
            default -> throw new IllegalArgumentException("unsupported knowledge base visibility");
        };
    }

    private String resolveVisibility(String visibility, String allowedRoles) {
        String normalizedVisibility = StringUtils.hasText(visibility) ? normalizeVisibility(visibility) : null;
        if (!StringUtils.hasText(normalizedVisibility)) {
            return StringUtils.hasText(allowedRoles) ? "ROLES" : "PRIVATE";
        }
        if ("PRIVATE".equals(normalizedVisibility) && StringUtils.hasText(allowedRoles)) {
            return "ROLES";
        }
        return normalizedVisibility;
    }

    private String normalizeAllowedRoles(String allowedRoles) {
        String normalized = RoleUtils.normalizeRoleExpression(blankToNull(allowedRoles));
        return normalized;
    }

    private void validateRoleScope(String visibility, String allowedRoles) {
        if ("ROLES".equalsIgnoreCase(visibility) && !StringUtils.hasText(allowedRoles)) {
            throw new IllegalArgumentException("allowed roles are required when visibility is ROLES");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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


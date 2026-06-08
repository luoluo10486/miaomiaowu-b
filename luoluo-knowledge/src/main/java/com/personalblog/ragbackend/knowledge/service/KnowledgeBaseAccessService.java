package com.personalblog.ragbackend.knowledge.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalblog.ragbackend.common.auth.RoleUtils;
import com.personalblog.ragbackend.common.context.UserContext;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeBaseDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentDO;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识BaseAccess服务
 */
@Service
public class KnowledgeBaseAccessService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public KnowledgeBaseAccessService(KnowledgeBaseMapper knowledgeBaseMapper,
                                      KnowledgeDocumentMapper knowledgeDocumentMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
    }

    public boolean canRead(KnowledgeBaseDO knowledgeBase) {
        if (knowledgeBase == null || isDeleted(knowledgeBase)) {
            return false;
        }
        if (isSuperAdmin()) {
            return true;
        }
        if (isOwner(knowledgeBase)) {
            return true;
        }
        if (!"ACTIVE".equals(normalizeStatus(knowledgeBase.getStatus()))) {
            return false;
        }
        String visibility = normalizeVisibility(knowledgeBase.getVisibility());
        if ("PUBLIC".equals(visibility)) {
            return true;
        }
        Set<String> allowedRoles = allowedRoles(knowledgeBase);
        return !allowedRoles.isEmpty() && hasIntersection(currentRoles(), allowedRoles);
    }

    public boolean canManage(KnowledgeBaseDO knowledgeBase) {
        if (knowledgeBase == null || isDeleted(knowledgeBase)) {
            return false;
        }
        if (isSuperAdmin()) {
            return true;
        }
        return isOwner(knowledgeBase);
    }

    public boolean canRead(Long kbId) {
        return canRead(findById(kbId));
    }

    public boolean canManage(Long kbId) {
        return canManage(findById(kbId));
    }

    public boolean canRead(String kbId) {
        return canRead(parseId(kbId));
    }

    public boolean canManage(String kbId) {
        return canManage(parseId(kbId));
    }

    public boolean canReadByCollectionName(String collectionName) {
        return canRead(findByCollectionName(collectionName));
    }

    public boolean canManageByCollectionName(String collectionName) {
        return canManage(findByCollectionName(collectionName));
    }

    public boolean canReadDocument(String docId) {
        return canRead(findDocumentKnowledgeBase(docId));
    }

    public boolean canManageDocument(String docId) {
        return canManage(findDocumentKnowledgeBase(docId));
    }

    public boolean canRead(KnowledgeDocumentDO document) {
        if (document == null) {
            return false;
        }
        return canRead(findById(document.getKbId()));
    }

    public boolean canManage(KnowledgeDocumentDO document) {
        if (document == null) {
            return false;
        }
        return canManage(findById(document.getKbId()));
    }

    public void assertReadable(KnowledgeBaseDO knowledgeBase) {
        if (!canRead(knowledgeBase)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
    }

    public void assertReadable(Long kbId) {
        if (!canRead(kbId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
    }

    public void assertReadable(String kbId) {
        if (!canRead(kbId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
    }

    public void assertManageable(KnowledgeBaseDO knowledgeBase) {
        if (!canManage(knowledgeBase)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
    }

    public void assertManageable(Long kbId) {
        if (!canManage(kbId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
    }

    public void assertManageable(String kbId) {
        if (!canManage(kbId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
    }

    public void assertReadableDocument(String docId) {
        if (!canReadDocument(docId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
    }

    public void assertManageableDocument(String docId) {
        if (!canManageDocument(docId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
    }

    public List<KnowledgeBaseDO> filterReadable(Collection<KnowledgeBaseDO> knowledgeBases) {
        if (CollUtil.isEmpty(knowledgeBases)) {
            return List.of();
        }
        return knowledgeBases.stream()
                .filter(Objects::nonNull)
                .filter(this::canRead)
                .collect(Collectors.toList());
    }

    public Set<String> readableCollectionNames() {
        List<KnowledgeBaseDO> knowledgeBases = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseDO>()
                        .eq(KnowledgeBaseDO::getDeleted, 0)
        );
        if (CollUtil.isEmpty(knowledgeBases)) {
            return Set.of();
        }
        return knowledgeBases.stream()
                .filter(this::canRead)
                .map(KnowledgeBaseDO::getCollectionName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private KnowledgeBaseDO findById(Long kbId) {
        if (kbId == null) {
            return null;
        }
        return knowledgeBaseMapper.selectById(kbId);
    }

    private KnowledgeBaseDO findByCollectionName(String collectionName) {
        if (!StringUtils.hasText(collectionName)) {
            return null;
        }
        return knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBaseDO>()
                .eq(KnowledgeBaseDO::getCollectionName, collectionName.trim())
                .eq(KnowledgeBaseDO::getDeleted, 0)
                .last("limit 1"));
    }

    private KnowledgeDocumentDO findDocumentById(String docId) {
        Long documentId = parseId(docId);
        if (documentId == null) {
            return null;
        }
        return knowledgeDocumentMapper.selectById(documentId);
    }

    private KnowledgeBaseDO findDocumentKnowledgeBase(String docId) {
        KnowledgeDocumentDO document = findDocumentById(docId);
        if (document == null) {
            return null;
        }
        return findById(document.getKbId());
    }

    private Long parseId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSuperAdmin() {
        return RoleUtils.isSuperAdmin(UserContext.getRole());
    }

    private Set<String> currentRoles() {
        return RoleUtils.parseRoles(UserContext.getRole());
    }

    private Set<String> allowedRoles(KnowledgeBaseDO knowledgeBase) {
        return RoleUtils.parseRoles(knowledgeBase == null ? null : knowledgeBase.getAllowedRoles());
    }

    private boolean isOwner(KnowledgeBaseDO knowledgeBase) {
        if (knowledgeBase == null || knowledgeBase.getOwnerUserId() == null) {
            return false;
        }
        Long currentUserId = parseCurrentUserId();
        return currentUserId != null && knowledgeBase.getOwnerUserId().equals(currentUserId);
    }

    private Long parseCurrentUserId() {
        String userId = UserContext.getUserId();
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            return Long.valueOf(userId.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasIntersection(Set<String> left, Set<String> right) {
        if (CollUtil.isEmpty(left) || CollUtil.isEmpty(right)) {
            return false;
        }
        for (String role : left) {
            if (right.contains(role)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeVisibility(String visibility) {
        if (!StringUtils.hasText(visibility)) {
            return "PRIVATE";
        }
        return visibility.trim().toUpperCase();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        return status.trim().toUpperCase();
    }

    private boolean isDeleted(KnowledgeBaseDO knowledgeBase) {
        return knowledgeBase.getDeleted() != null && knowledgeBase.getDeleted() != 0;
    }
}

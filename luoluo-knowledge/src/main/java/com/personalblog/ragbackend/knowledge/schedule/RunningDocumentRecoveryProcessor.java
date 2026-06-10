package com.personalblog.ragbackend.knowledge.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalblog.ragbackend.knowledge.config.KnowledgeScheduleProperties;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentChunkLogDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentDO;
import com.personalblog.ragbackend.knowledge.domain.enums.DocumentStatus;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentChunkLogMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Recovers documents that were left in RUNNING after a crash or forced restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunningDocumentRecoveryProcessor {
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeDocumentChunkLogMapper knowledgeDocumentChunkLogMapper;
    private final KnowledgeScheduleProperties scheduleProperties;

    @Transactional
    public int recover() {
        return recoverInternal(null);
    }

    @Transactional
    public int recoverKnowledgeBase(Long kbId) {
        return recoverInternal(kbId);
    }

    private int recoverInternal(Long kbId) {
        long timeoutMinutes = Math.max(1L, scheduleProperties.getRunningTimeoutMinutes() == null
                ? 120L
                : scheduleProperties.getRunningTimeoutMinutes());
        int batchSize = Math.max(1, scheduleProperties.getBatchSize() == null
                ? 20
                : scheduleProperties.getBatchSize());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(timeoutMinutes);
        String errorMessage = "chunk task timed out or service restarted before completion";

        LambdaQueryWrapper<KnowledgeDocumentDO> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentDO>()
                .eq(KnowledgeDocumentDO::getDeleted, 0)
                .eq(KnowledgeDocumentDO::getStatus, DocumentStatus.RUNNING.getCode())
                .eq(kbId != null, KnowledgeDocumentDO::getKbId, kbId)
                .le(KnowledgeDocumentDO::getUpdatedAt, cutoff)
                .orderByAsc(KnowledgeDocumentDO::getUpdatedAt)
                .last("LIMIT " + batchSize);
        List<KnowledgeDocumentDO> staleDocuments = knowledgeDocumentMapper.selectList(queryWrapper);
        if (staleDocuments == null || staleDocuments.isEmpty()) {
            return 0;
        }

        for (KnowledgeDocumentDO document : staleDocuments) {
            markDocumentFailed(document, now, errorMessage);
            markRunningLogsFailed(document.getId(), now, errorMessage);
        }

        log.warn("Recovered stale running knowledge documents, kbId={}, count={}, timeoutMinutes={}",
                kbId, staleDocuments.size(), timeoutMinutes);
        return staleDocuments.size();
    }

    private void markDocumentFailed(KnowledgeDocumentDO document, LocalDateTime now, String errorMessage) {
        KnowledgeDocumentDO update = new KnowledgeDocumentDO();
        update.setId(document.getId());
        update.setStatus(DocumentStatus.FAILED.getCode());
        update.setErrorMessage(errorMessage);
        update.setUpdatedAt(now);
        knowledgeDocumentMapper.updateById(update);
    }

    private void markRunningLogsFailed(Long docId, LocalDateTime now, String errorMessage) {
        if (docId == null) {
            return;
        }
        List<KnowledgeDocumentChunkLogDO> runningLogs = knowledgeDocumentChunkLogMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocumentChunkLogDO>()
                        .eq(KnowledgeDocumentChunkLogDO::getDocId, docId)
                        .eq(KnowledgeDocumentChunkLogDO::getStatus, DocumentStatus.RUNNING.getCode())
                        .orderByDesc(KnowledgeDocumentChunkLogDO::getStartedAt)
        );
        if (runningLogs == null || runningLogs.isEmpty()) {
            return;
        }
        for (KnowledgeDocumentChunkLogDO logEntity : runningLogs) {
            KnowledgeDocumentChunkLogDO update = new KnowledgeDocumentChunkLogDO();
            update.setId(logEntity.getId());
            update.setStatus(DocumentStatus.FAILED.getCode());
            update.setErrorMessage(errorMessage);
            update.setEndedAt(now);
            update.setUpdatedAt(now);
            knowledgeDocumentChunkLogMapper.updateById(update);
        }
    }
}

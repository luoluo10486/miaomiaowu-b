package com.personalblog.ragbackend.knowledge.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 知识文档调度定时任务
 */
@Component
@RequiredArgsConstructor
public class KnowledgeDocumentScheduleJob {
    private final RunningDocumentRecoveryProcessor runningDocumentRecoveryProcessor;
    private final ScheduleRefreshProcessor scheduleRefreshProcessor;

    @Scheduled(fixedDelayString = "${rag.knowledge.schedule.scan-delay-ms:10000}")
    public void refresh() {
        runningDocumentRecoveryProcessor.recover();
        scheduleRefreshProcessor.refresh();
    }
}

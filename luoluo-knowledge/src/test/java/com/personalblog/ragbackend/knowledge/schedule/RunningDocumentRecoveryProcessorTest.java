package com.personalblog.ragbackend.knowledge.schedule;

import com.personalblog.ragbackend.knowledge.config.KnowledgeScheduleProperties;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentChunkLogDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentDO;
import com.personalblog.ragbackend.knowledge.domain.enums.DocumentStatus;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentChunkLogMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunningDocumentRecoveryProcessorTest {

    @Mock
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Mock
    private KnowledgeDocumentChunkLogMapper knowledgeDocumentChunkLogMapper;

    private RunningDocumentRecoveryProcessor processor;

    @BeforeEach
    void setUp() {
        KnowledgeScheduleProperties properties = new KnowledgeScheduleProperties();
        properties.setBatchSize(20);
        properties.setRunningTimeoutMinutes(120L);
        processor = new RunningDocumentRecoveryProcessor(
                knowledgeDocumentMapper,
                knowledgeDocumentChunkLogMapper,
                properties
        );
    }

    @Test
    void shouldRecoverTimedOutRunningDocumentsAndLogs() {
        KnowledgeDocumentDO document = new KnowledgeDocumentDO();
        document.setId(64L);
        document.setStatus(DocumentStatus.RUNNING.getCode());
        document.setUpdatedAt(LocalDateTime.now().minusHours(1));

        KnowledgeDocumentChunkLogDO logEntity = new KnowledgeDocumentChunkLogDO();
        logEntity.setId(128L);
        logEntity.setDocId(64L);
        logEntity.setStatus(DocumentStatus.RUNNING.getCode());

        when(knowledgeDocumentMapper.selectList(any())).thenReturn(List.of(document));
        when(knowledgeDocumentChunkLogMapper.selectList(any())).thenReturn(List.of(logEntity));

        int recovered = processor.recover();

        assertThat(recovered).isEqualTo(1);

        ArgumentCaptor<KnowledgeDocumentDO> documentCaptor = ArgumentCaptor.forClass(KnowledgeDocumentDO.class);
        verify(knowledgeDocumentMapper).updateById(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getId()).isEqualTo(64L);
        assertThat(documentCaptor.getValue().getStatus()).isEqualTo(DocumentStatus.FAILED.getCode());
        assertThat(documentCaptor.getValue().getErrorMessage()).contains("timed out");

        ArgumentCaptor<KnowledgeDocumentChunkLogDO> logCaptor = ArgumentCaptor.forClass(KnowledgeDocumentChunkLogDO.class);
        verify(knowledgeDocumentChunkLogMapper, times(1)).updateById(logCaptor.capture());
        assertThat(logCaptor.getValue().getId()).isEqualTo(128L);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(DocumentStatus.FAILED.getCode());
        assertThat(logCaptor.getValue().getErrorMessage()).contains("timed out");
        assertThat(logCaptor.getValue().getEndedAt()).isNotNull();
    }

    @Test
    void shouldSkipWhenNoStaleRunningDocumentsExist() {
        when(knowledgeDocumentMapper.selectList(any())).thenReturn(List.of());

        int recovered = processor.recover();

        assertThat(recovered).isZero();
        verify(knowledgeDocumentMapper, times(0)).updateById(any(KnowledgeDocumentDO.class));
        verify(knowledgeDocumentChunkLogMapper, times(0)).updateById(any(KnowledgeDocumentChunkLogDO.class));
    }
}

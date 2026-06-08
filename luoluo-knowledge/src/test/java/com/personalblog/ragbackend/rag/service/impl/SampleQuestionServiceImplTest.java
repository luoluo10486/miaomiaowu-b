package com.personalblog.ragbackend.rag.service.impl;

import com.personalblog.ragbackend.knowledge.service.KnowledgeBaseAccessService;
import com.personalblog.ragbackend.rag.dao.entity.SampleQuestionEntity;
import com.personalblog.ragbackend.rag.dao.mapper.SampleQuestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SampleQuestionServiceImplTest {

    @Mock
    private SampleQuestionMapper sampleQuestionMapper;

    @Mock
    private KnowledgeBaseAccessService knowledgeBaseAccessService;

    private SampleQuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SampleQuestionServiceImpl(sampleQuestionMapper, knowledgeBaseAccessService);
    }

    @Test
    void listHomepageQuestionsShouldHideQuestionsLinkedToUnreadableKnowledgeBases() {
        SampleQuestionEntity publicQuestion = sampleQuestion(1L, null, "公开样例", 1);
        SampleQuestionEntity readableQuestion = sampleQuestion(2L, 10L, "可读样例", 2);
        SampleQuestionEntity hiddenQuestion = sampleQuestion(3L, 20L, "隐藏样例", 3);

        when(sampleQuestionMapper.selectList(any())).thenReturn(List.of(publicQuestion, readableQuestion, hiddenQuestion));
        when(knowledgeBaseAccessService.canRead(10L)).thenReturn(true);
        when(knowledgeBaseAccessService.canRead(20L)).thenReturn(false);

        List<?> result = service.listHomepageQuestions();

        assertThat(result)
                .hasSize(2)
                .extracting("title")
                .containsExactly("公开样例", "可读样例");
    }

    private SampleQuestionEntity sampleQuestion(Long id, Long kbId, String title, int sortOrder) {
        SampleQuestionEntity entity = new SampleQuestionEntity();
        entity.setId(id);
        entity.setKbId(kbId);
        entity.setTitle(title);
        entity.setDescription(title + " 描述");
        entity.setQuestion(title + " 问题");
        entity.setSortOrder(sortOrder);
        entity.setEnabled(1);
        entity.setDeleted(0);
        return entity;
    }
}

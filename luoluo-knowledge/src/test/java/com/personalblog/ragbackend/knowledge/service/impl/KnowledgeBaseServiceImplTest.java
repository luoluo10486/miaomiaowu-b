package com.personalblog.ragbackend.knowledge.service.impl;

import com.personalblog.ragbackend.common.context.LoginUser;
import com.personalblog.ragbackend.common.context.UserContext;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeBaseCreateRequest;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeBaseDO;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentMapper;
import com.personalblog.ragbackend.knowledge.service.KnowledgeBaseAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识Base服务ImplTest类
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Mock
    private KnowledgeBaseAccessService knowledgeBaseAccessService;

    private KnowledgeBaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseServiceImpl(knowledgeBaseMapper, knowledgeDocumentMapper, knowledgeBaseAccessService);
        LoginUser user = new LoginUser();
        user.setUserId("42");
        UserContext.set(user);

        when(knowledgeBaseMapper.selectCount(any())).thenReturn(0L);
        when(knowledgeBaseMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            KnowledgeBaseDO entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        }).when(knowledgeBaseMapper).insert(any(KnowledgeBaseDO.class));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createShouldAutoGenerateCollectionNameWhenBlank() {
        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
        request.setName("Luoluo RAG");
        request.setEmbeddingModel(null);
        request.setCollectionName("   ");

        String id = service.create(request);

        assertThat(id).isEqualTo("99");

        ArgumentCaptor<KnowledgeBaseDO> entityCaptor = ArgumentCaptor.forClass(KnowledgeBaseDO.class);
        verify(knowledgeBaseMapper).insert(entityCaptor.capture());
        KnowledgeBaseDO saved = entityCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Luoluo RAG");
        assertThat(saved.getCollectionName()).startsWith("kb_");
        assertThat(saved.getCollectionName()).isNotBlank();
        assertThat(saved.getEmbeddingModel()).isEqualTo("Qwen/Qwen3-Embedding-8B");
        assertThat(saved.getOwnerUserId()).isEqualTo(42L);
    }

    @Test
    void createShouldKeepExplicitCollectionName() {
        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
        request.setName("HR Policy");
        request.setEmbeddingModel("Qwen/Qwen3-Embedding-8B");
        request.setCollectionName("kb_hr_policy");

        service.create(request);

        ArgumentCaptor<KnowledgeBaseDO> entityCaptor = ArgumentCaptor.forClass(KnowledgeBaseDO.class);
        verify(knowledgeBaseMapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getCollectionName()).isEqualTo("kb_hr_policy");
    }
}

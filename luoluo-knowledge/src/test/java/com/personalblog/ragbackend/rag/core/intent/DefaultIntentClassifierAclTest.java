package com.personalblog.ragbackend.rag.core.intent;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalblog.ragbackend.infra.chat.LLMService;
import com.personalblog.ragbackend.infra.convention.ChatRequest;
import com.personalblog.ragbackend.knowledge.service.KnowledgeBaseAccessService;
import com.personalblog.ragbackend.rag.core.prompt.PromptTemplateLoader;
import com.personalblog.ragbackend.rag.dao.entity.IntentNodeEntity;
import com.personalblog.ragbackend.rag.dao.mapper.IntentNodeMapper;
import com.personalblog.ragbackend.rag.enums.IntentKind;
import com.personalblog.ragbackend.rag.enums.IntentLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultIntentClassifierAclTest {
    @Mock
    private LLMService llmService;

    @Mock
    private IntentNodeMapper intentNodeMapper;

    @Mock
    private PromptTemplateLoader promptTemplateLoader;

    @Mock
    private IntentTreeCacheManager intentTreeCacheManager;

    @Mock
    private KnowledgeBaseAccessService knowledgeBaseAccessService;

    private DefaultIntentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new DefaultIntentClassifier(
                llmService,
                intentNodeMapper,
                promptTemplateLoader,
                intentTreeCacheManager,
                new ObjectMapper(),
                knowledgeBaseAccessService
        );
    }

    @Test
    void classifyTargetsShouldExcludeUnreadableKbLeavesFromPromptAndResults() {
        IntentNodeEntity readable = kbLeaf(1L, "java_kb", "Java 知识文档", "java_domain");
        IntentNodeEntity unreadable = kbLeaf(2L, "qq_chat", "QQ 聊天记录", "chat_domain");

        when(intentTreeCacheManager.getIntentTreeFromCache()).thenReturn(List.of());
        when(intentNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(readable, unreadable));
        when(knowledgeBaseAccessService.canRead("1")).thenReturn(true);
        when(knowledgeBaseAccessService.canRead("2")).thenReturn(false);
        when(promptTemplateLoader.render(any(), any())).thenAnswer(invocation -> {
            Object value = invocation.getArgument(1, java.util.Map.class).get("intent_list");
            return String.valueOf(value);
        });
        when(llmService.chat(any(ChatRequest.class))).thenReturn("""
                [{"id":"java_kb","score":0.91},{"id":"qq_chat","score":0.95}]
                """);

        List<NodeScore> result = classifier.classifyTargets("Java 权限链路");

        verify(llmService).chat(any(ChatRequest.class));
        verify(promptTemplateLoader).render(any(), any());

        assertThat(result).extracting(score -> score.node().getIntentCode())
                .containsExactly("java_kb");
    }

    private IntentNodeEntity kbLeaf(Long kbId, String intentCode, String name, String parentCode) {
        IntentNodeEntity entity = new IntentNodeEntity();
        entity.setId(kbId + 100);
        entity.setKbId(kbId);
        entity.setIntentCode(intentCode);
        entity.setName(name);
        entity.setLevel(IntentLevel.TOPIC.getCode());
        entity.setParentCode(parentCode);
        entity.setKind(IntentKind.KB.getCode());
        entity.setEnabled(1);
        entity.setDeleted(0);
        entity.setSortOrder(0);
        entity.setCollectionName(intentCode + "_collection");
        return entity;
    }
}

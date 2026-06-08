package com.personalblog.ragbackend.rag.core.guidance;

import com.personalblog.ragbackend.knowledge.service.KnowledgeBaseAccessService;
import com.personalblog.ragbackend.rag.config.GuidanceProperties;
import com.personalblog.ragbackend.rag.core.intent.IntentNode;
import com.personalblog.ragbackend.rag.core.intent.IntentNodeRegistry;
import com.personalblog.ragbackend.rag.core.intent.NodeScore;
import com.personalblog.ragbackend.rag.core.intent.SubQuestionIntent;
import com.personalblog.ragbackend.rag.core.prompt.PromptTemplateLoader;
import com.personalblog.ragbackend.rag.enums.IntentKind;
import com.personalblog.ragbackend.rag.enums.IntentLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 意图引导服务AclTest类
 */
@ExtendWith(MockitoExtension.class)
class IntentGuidanceServiceAclTest {
    @Mock
    private IntentNodeRegistry intentNodeRegistry;

    @Mock
    private PromptTemplateLoader promptTemplateLoader;

    @Mock
    private AmbiguityLLMChecker ambiguityLLMChecker;

    @Mock
    private KnowledgeBaseAccessService knowledgeBaseAccessService;

    private IntentGuidanceService guidanceService;

    @BeforeEach
    void setUp() {
        GuidanceProperties properties = new GuidanceProperties();
        properties.setEnabled(true);
        guidanceService = new IntentGuidanceService(
                properties,
                intentNodeRegistry,
                promptTemplateLoader,
                ambiguityLLMChecker,
                knowledgeBaseAccessService
        );
    }

    @Test
    void detectAmbiguityShouldIgnoreUnreadableKbCandidates() {
        IntentNode readable = topicNode("java_topic", "Java Knowledge", "chat_category", "1");
        IntentNode unreadable = topicNode("qq_topic", "QQ Chat", "chat_category", "2");

        when(knowledgeBaseAccessService.canRead("1")).thenReturn(true);
        when(knowledgeBaseAccessService.canRead("2")).thenReturn(false);

        GuidanceDecision decision = guidanceService.detectAmbiguity(
                "who mentioned install issues",
                List.of(new SubQuestionIntent("who mentioned install issues", List.of(
                        NodeScore.builder().node(readable).score(0.92D).build(),
                        NodeScore.builder().node(unreadable).score(0.91D).build()
                )))
        );

        assertThat(decision.isPrompt()).isFalse();
    }

    private IntentNode topicNode(String id, String name, String parentId, String kbId) {
        IntentNode node = new IntentNode();
        node.setId(id);
        node.setName(name);
        node.setParentId(parentId);
        node.setKbId(kbId);
        node.setLevel(IntentLevel.TOPIC);
        node.setKind(IntentKind.KB);
        return node;
    }
}

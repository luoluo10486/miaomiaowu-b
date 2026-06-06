package com.personalblog.ragbackend.rag.core.prompt;

import com.personalblog.ragbackend.infra.convention.ChatMessage;
import com.personalblog.ragbackend.infra.convention.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RAGPromptServiceTest {

    @Mock
    private PromptTemplateLoader promptTemplateLoader;

    @Test
    void shouldIncludeChunkExcerptInCitationBody() {
        when(promptTemplateLoader.load(anyString())).thenReturn("");
        when(promptTemplateLoader.renderSection(anyString(), anyString(), anyMap()))
                .thenAnswer(invocation -> {
                    String section = invocation.getArgument(1, String.class);
                    Map<String, String> slots = invocation.getArgument(2, Map.class);
                    return "<" + section + ">" + slots + "</" + section + ">";
                });

        RAGPromptService service = new RAGPromptService(promptTemplateLoader);
        PromptContext context = PromptContext.builder()
                .kbContext("知识库上下文")
                .intentChunks(Map.of(
                        "kb-policy", List.of(
                                RetrievedChunk.builder()
                                        .id("1")
                                        .text("第一段相关片段，包含具体规则说明。第二句补充更多细节。")
                                        .score(0.91F)
                                        .metadata(Map.of(
                                                "title", "知识库文档 A",
                                                "sourceUrl", "https://example.com/doc-a",
                                                "chunkIndex", 1
                                        ))
                                        .build(),
                                RetrievedChunk.builder()
                                        .id("2")
                                        .text("第二段相关片段，说明同一文档下的另一个命中块。")
                                        .score(0.87F)
                                        .metadata(Map.of(
                                                "title", "知识库文档 A",
                                                "sourceUrl", "https://example.com/doc-a",
                                                "chunkIndex", 2
                                        ))
                                        .build()
                        )
                ))
                .build();

        List<ChatMessage> messages = service.buildStructuredMessages(
                context,
                List.of(),
                "知识库怎么写来源？",
                List.of()
        );

        assertThat(messages).hasSize(1);
        String userContent = messages.get(0).getContent();
        assertThat(userContent).contains("excerpt=第一段相关片段，包含具体规则说明。第二句补充更多细节。");
        assertThat(userContent).contains("excerpt=第二段相关片段，说明同一文档下的另一个命中块。");
        assertThat(userContent).contains("chunk_index=1");
        assertThat(userContent).contains("chunk_index=2");
    }
}

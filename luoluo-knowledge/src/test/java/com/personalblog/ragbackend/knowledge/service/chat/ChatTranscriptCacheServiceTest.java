package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.config.RagKnowledgeProcessingProperties;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatMessage;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTranscriptCacheServiceTest {

    @Test
    void cacheShouldStoreLightweightTranscriptAndReuseParsedMonthlyMessages() {
        RagKnowledgeProcessingProperties properties = new RagKnowledgeProcessingProperties();
        properties.setChatTranscriptCacheMaxEntries(1);
        properties.setChatTranscriptCacheTtlSeconds(300);

        ChatTranscriptCacheService service = new ChatTranscriptCacheService(properties);
        AtomicInteger parseCount = new AtomicInteger();
        ChatTranscriptParser parser = new ChatTranscriptParser() {
            @Override
            public String platform() {
                return "qq";
            }

            @Override
            public String docType() {
                return "chat_qq_group";
            }

            @Override
            public QqChatTranscript parse(byte[] bytes, String fileName) {
                parseCount.incrementAndGet();
                return new QqChatTranscript(
                        fileName,
                        "qq",
                        "chat_qq_group",
                        "Demo Group",
                        "群聊",
                        null,
                        2,
                        null,
                        null,
                        List.of(
                                new QqChatMessage("Alice", "Alice", LocalDateTime.of(2022, 1, 1, 10, 0), "hello", 1, 1, 1),
                                new QqChatMessage("Bob", "Bob", LocalDateTime.of(2022, 1, 2, 10, 0), "world", 2, 2, 2)
                        )
                );
            }
        };

        ChatTranscriptCacheService.CachedTranscript first = service.getOrParse("cache-key", new byte[]{1, 2, 3}, "chat.txt", parser);
        ChatTranscriptCacheService.CachedTranscript second = service.getOrParse("cache-key", new byte[]{4, 5, 6}, "chat.txt", parser);

        assertThat(parseCount.get()).isEqualTo(1);
        assertThat(first.transcript().messages()).isEmpty();
        assertThat(second.transcript().messages()).isEmpty();
        assertThat(second.monthlyMessages("2022-01")).hasSize(2);
        assertThat(second.transcript().groupName()).isEqualTo("Demo Group");
    }
}

package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.dto.chat.QqChatMessage;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import com.personalblog.ragbackend.knowledge.dto.document.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTranscriptChunkServiceTest {

    private final ChatTranscriptChunkService service = new ChatTranscriptChunkService();

    @Test
    void chunkTranscriptShouldKeepMonthlyWindowMetadataAndOverlap() {
        QqChatTranscript transcript = new QqChatTranscript(
                "qq-chat.txt",
                "qq",
                "chat_qq_group",
                "Demo Group",
                "\u7fa4\u804a",
                null,
                8,
                null,
                null,
                List.of(
                        message("Alice", LocalDateTime.of(2022, 1, 26, 10, 0, 46), "hello", 1, 12, 14),
                        message("Bob", LocalDateTime.of(2022, 1, 26, 10, 1, 6), "coming", 2, 16, 18),
                        message("Alice", LocalDateTime.of(2022, 1, 26, 10, 1, 30), "open the box", 3, 20, 22),
                        message("Bob", LocalDateTime.of(2022, 1, 26, 10, 5, 0), "me too", 4, 24, 26),
                        message("Carol", LocalDateTime.of(2022, 1, 26, 10, 7, 0), "same here", 5, 28, 30),
                        message("Alice", LocalDateTime.of(2022, 1, 26, 10, 8, 0), "shared link", 6, 32, 34),
                        message("Bob", LocalDateTime.of(2022, 1, 26, 10, 10, 0), "received", 7, 36, 38),
                        message("Alice", LocalDateTime.of(2022, 2, 1, 9, 0, 0), "new month", 8, 40, 42)
                )
        );

        ChatChunkingOptions options = new ChatChunkingOptions(2, 3, 1, 900, 1200, 30, 20);
        List<DocumentChunk> chunks = service.chunkTranscript(transcript, "2022-01", options);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).content()).contains("[2022-01-26 10:00:46] Alice: hello");
        assertThat(chunks.get(1).content()).contains("[2022-01-26 10:01:30] Alice: open the box");
        assertThat(chunks.get(2).content()).contains("[2022-01-26 10:08:00] Alice: shared link");
        assertThat(chunks.get(0).metadata()).containsEntry("docType", "chat_qq_group");
        assertThat(chunks.get(0).metadata()).containsEntry("chatPlatform", "qq");
        assertThat(chunks.get(0).metadata()).containsEntry("bucketMonth", "2022-01");
        assertThat(chunks.get(0).metadata()).containsEntry("messageStartIndex", 1);
        assertThat(chunks.get(2).metadata()).containsEntry("messageEndIndex", 7);
        assertThat(chunks.get(1).metadata().get("speakerSet")).asList().contains("Alice", "Bob", "Carol");
        assertThat(chunks.get(2).metadata()).containsEntry("sourceFile", "qq-chat.txt");
    }

    private QqChatMessage message(String speakerName,
                                  LocalDateTime timestamp,
                                  String content,
                                  int messageIndex,
                                  int lineStart,
                                  int lineEnd) {
        return new QqChatMessage(
                speakerName,
                speakerName,
                timestamp,
                content,
                messageIndex,
                lineStart,
                lineEnd
        );
    }
}

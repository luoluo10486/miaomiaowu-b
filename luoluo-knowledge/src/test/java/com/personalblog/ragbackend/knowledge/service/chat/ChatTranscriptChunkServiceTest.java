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
                "%御魂师～@",
                "群聊",
                null,
                8,
                null,
                null,
                List.of(
                        message("会长", LocalDateTime.of(2022, 1, 26, 10, 0, 46), "来抽卡", 1, 12, 14),
                        message("橙", LocalDateTime.of(2022, 1, 26, 10, 1, 6), "这么快", 2, 16, 18),
                        message("会长", LocalDateTime.of(2022, 1, 26, 10, 1, 30), "先开箱子", 3, 20, 22),
                        message("橙", LocalDateTime.of(2022, 1, 26, 10, 5, 0), "我也抽", 4, 24, 26),
                        message("路人甲", LocalDateTime.of(2022, 1, 26, 10, 7, 0), "俺也去", 5, 28, 30),
                        message("会长", LocalDateTime.of(2022, 1, 26, 10, 8, 0), "发下链接", 6, 32, 34),
                        message("橙", LocalDateTime.of(2022, 1, 26, 10, 10, 0), "收到", 7, 36, 38),
                        message("会长", LocalDateTime.of(2022, 2, 1, 9, 0, 0), "二月新话题", 8, 40, 42)
                )
        );

        ChatChunkingOptions options = new ChatChunkingOptions(2, 3, 1, 900, 1200, 30, 20);
        List<DocumentChunk> chunks = service.chunkTranscript(transcript, "2022-01", options);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).content()).contains("[2022-01-26 10:00:46] 会长: 来抽卡");
        assertThat(chunks.get(1).content()).contains("[2022-01-26 10:01:30] 会长: 先开箱子");
        assertThat(chunks.get(2).content()).contains("[2022-01-26 10:08:00] 会长: 发下链接");
        assertThat(chunks.get(0).metadata()).containsEntry("bucketMonth", "2022-01");
        assertThat(chunks.get(0).metadata()).containsEntry("messageStartIndex", 1);
        assertThat(chunks.get(2).metadata()).containsEntry("messageEndIndex", 7);
        assertThat(chunks.get(1).metadata().get("speakerSet")).asList().contains("会长", "橙", "路人甲");
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

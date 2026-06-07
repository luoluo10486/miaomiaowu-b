package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class QqChatTranscriptParserTest {

    private final QqChatTranscriptParser parser = new QqChatTranscriptParser();

    @Test
    void parseShouldSupportUtf8Transcript() {
        QqChatTranscript transcript = parser.parse(sampleTranscript().getBytes(StandardCharsets.UTF_8), "qq-chat.txt");

        assertThat(transcript.platform()).isEqualTo("qq");
        assertThat(transcript.docType()).isEqualTo("chat_qq_group");
        assertThat(transcript.groupName()).isEqualTo("Demo Group");
        assertThat(transcript.chatType()).isEqualTo("\u7fa4\u804a");
        assertThat(transcript.messageTotal()).isEqualTo(3);
        assertThat(transcript.messages()).hasSize(3);
        assertThat(transcript.messages().get(0).speakerName()).isEqualTo("Alice");
        assertThat(transcript.messages().get(1).speakerName()).isEqualTo("Bob");
        assertThat(transcript.messages().get(2).content()).isEqualTo("download link");
    }

    @Test
    void parseShouldSupportGb18030Transcript() {
        byte[] bytes = sampleTranscript().getBytes(Charset.forName("GB18030"));

        QqChatTranscript transcript = parser.parse(bytes, "qq-chat-gbk.txt");

        assertThat(transcript.platform()).isEqualTo("qq");
        assertThat(transcript.groupName()).isEqualTo("Demo Group");
        assertThat(transcript.messages()).hasSize(3);
        assertThat(transcript.messages().get(2).content()).isEqualTo("download link");
    }

    private String sampleTranscript() {
        return """
                [QQChatExporter V5 / https://github.com/shuakami/qq-chat-exporter]

                ===============================================
                           QQ\u804a\u5929\u8bb0\u5f55\u5bfc\u51fa\u6587\u4ef6
                ===============================================

                \u804a\u5929\u540d\u79f0: Demo Group
                \u804a\u5929\u7c7b\u578b: \u7fa4\u804a
                \u5bfc\u51fa\u65f6\u95f4: 2026-06-07 09:58:57
                \u6d88\u606f\u603b\u6570: 3
                \u65f6\u95f4\u8303\u56f4: 2022-01-26 10:00:46 - 2022-01-26 10:06:00

                [owner] Alice:
                \u65f6\u95f4: 2022-01-26 10:00:46
                \u5185\u5bb9: hello @Bob
                [member] Bob:
                \u65f6\u95f4: 2022-01-26 10:01:06
                \u5185\u5bb9: coming
                [owner] Alice:
                \u65f6\u95f4: 2022-01-26 10:06:00
                \u5185\u5bb9: download link
                """;
    }
}

package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WechatChatTranscriptParserTest {

    private final WechatChatTranscriptParser parser = new WechatChatTranscriptParser();

    @Test
    void parseShouldSupportWechatGroupTranscript() {
        QqChatTranscript transcript = parser.parse(sampleTranscript().getBytes(StandardCharsets.UTF_8), "wechat-chat.txt");

        assertThat(transcript.platform()).isEqualTo("wechat");
        assertThat(transcript.docType()).isEqualTo("chat_wechat_group");
        assertThat(transcript.groupName()).isEqualTo("impart");
        assertThat(transcript.chatType()).isEqualTo("\u7fa4\u804a");
        assertThat(transcript.messageTotal()).isEqualTo(4);
        assertThat(transcript.exportedAt()).isNotNull();
        assertThat(transcript.messages()).hasSize(4);
        assertThat(transcript.messages().get(0).speakerName()).isEqualTo("me");
        assertThat(transcript.messages().get(1).speakerName()).isEqualTo("Alice");
        assertThat(transcript.messages().get(2).speakerName()).isEqualTo("[\u7cfb\u7edf]");
        assertThat(transcript.messages().get(2).content()).contains("impart");
        assertThat(transcript.messages().get(3).content()).isEqualTo("see you");
    }

    private String sampleTranscript() {
        return """
                \u804a\u5929\u8bb0\u5f55: impart
                \u7c7b\u578b: \u7fa4\u804a
                \u65f6\u95f4\u8303\u56f4: \u6700\u65e9 ~ \u6700\u65b0
                \u5bfc\u51fa\u65f6\u95f4: 2026-06-07 16:58
                \u6d88\u606f\u6570\u91cf: 4
                ============================================================
                [2024-05-04 14:34] me: start now
                [2024-05-04 14:34] Alice: ok
                [2024-05-04 14:34] [\u7cfb\u7edf] \u4f60\u4fee\u6539\u7fa4\u540d\u4e3a\u201cimpart\u201d
                [2024-05-04 14:35] Bob: see you
                """;
    }
}

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

        assertThat(transcript.groupName()).isEqualTo("%御魂师～@");
        assertThat(transcript.chatType()).isEqualTo("群聊");
        assertThat(transcript.messageTotal()).isEqualTo(3);
        assertThat(transcript.messages()).hasSize(3);
        assertThat(transcript.messages().get(0).speakerName()).isEqualTo("会长");
        assertThat(transcript.messages().get(1).speakerName()).isEqualTo("橙");
        assertThat(transcript.messages().get(2).content()).contains("游戏下载");
    }

    @Test
    void parseShouldSupportGb18030Transcript() {
        byte[] bytes = sampleTranscript().getBytes(Charset.forName("GB18030"));

        QqChatTranscript transcript = parser.parse(bytes, "qq-chat-gbk.txt");

        assertThat(transcript.groupName()).isEqualTo("%御魂师～@");
        assertThat(transcript.messages()).hasSize(3);
        assertThat(transcript.messages().get(2).content()).isEqualTo("我晚点发游戏下载地址");
    }

    private String sampleTranscript() {
        return """
                [QQChatExporter V5 / https://github.com/shuakami/qq-chat-exporter]

                ===============================================
                           QQ聊天记录导出文件
                ===============================================

                聊天名称: %御魂师～@
                聊天类型: 群聊
                导出时间: 2026-06-07 09:58:57
                消息总数: 3
                时间范围: 2022-01-26 10:00:46 - 2022-01-26 10:06:00

                [会长] 会长:
                时间: 2022-01-26 10:00:46
                内容: @橙 来抽卡

                [陈独秀] 橙:
                时间: 2022-01-26 10:01:06
                内容: 这么快

                [会长] 会长:
                时间: 2022-01-26 10:06:00
                内容: 我晚点发游戏下载地址
                """;
    }
}

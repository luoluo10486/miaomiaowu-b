package com.personalblog.ragbackend.rag.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownContentSanitizerTest {

    @Test
    void shouldRemoveToolCallArtifactsFromContent() {
        String content = """
                前面说明
                <tool-call>
                  <function=weather_query>
                    <parameter>{"city":"北京"}</parameter>
                  </function>
                </tool-call>
                后面结论
                """;

        String sanitized = MarkdownContentSanitizer.stripImagesAndToolCallArtifacts(content);

        assertThat(sanitized).contains("前面说明");
        assertThat(sanitized).contains("后面结论");
        assertThat(sanitized).doesNotContain("<tool-call>");
        assertThat(sanitized).doesNotContain("weather_query");
        assertThat(sanitized).doesNotContain("<parameter>");
    }

    @Test
    void shouldKeepNormalMarkdownUntouched() {
        String content = """
                # 标题

                正常内容
                """;

        String sanitized = MarkdownContentSanitizer.stripImagesAndToolCallArtifacts(content);

        assertThat(sanitized).contains("# 标题");
        assertThat(sanitized).contains("正常内容");
    }
}

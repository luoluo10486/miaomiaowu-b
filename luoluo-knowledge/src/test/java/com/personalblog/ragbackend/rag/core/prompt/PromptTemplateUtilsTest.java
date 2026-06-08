package com.personalblog.ragbackend.rag.core.prompt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 提示词Template工具类Test类
 */
class PromptTemplateUtilsTest {

    @Test
    void contextFormatTemplateShouldContainCitationSections() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("prompt/context-format.st")) {
            assertThat(inputStream).isNotNull();
            String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            Map<String, String> sections = PromptTemplateUtils.parseSections(template);

            assertThat(sections).containsKeys("kb-citation-item", "kb-citations");
            assertThat(sections.get("kb-citation-item")).contains("<citation index=\"{index}\">");
            assertThat(sections.get("kb-citations")).contains("<citations>");
        }
    }
}

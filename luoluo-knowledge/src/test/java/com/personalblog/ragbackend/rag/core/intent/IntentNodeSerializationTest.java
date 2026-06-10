package com.personalblog.ragbackend.rag.core.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalblog.ragbackend.rag.enums.IntentKind;
import com.personalblog.ragbackend.rag.enums.IntentLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentNodeSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeIntentNodeWithoutDerivedKbFlags() throws Exception {
        IntentNode node = IntentNode.builder()
                .id("weather_today")
                .name("Weather Today")
                .level(IntentLevel.TOPIC)
                .kind(IntentKind.KB)
                .build();

        String json = objectMapper.writeValueAsString(node);

        assertThat(json).contains("\"intentCode\":\"weather_today\"");
        assertThat(json).doesNotContain("\"kb\":");
        assertThat(json).doesNotContain("\"KB\":");
    }
}

package com.personalblog.ragbackend.rag.core.intent;

import com.personalblog.ragbackend.rag.enums.IntentKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntentTreeFactoryWeatherTest {

    @Test
    void shouldContainWeatherMcpNode() {
        List<IntentNode> roots = IntentTreeFactory.buildIntentTree();
        List<IntentNode> allNodes = flatten(roots);

        IntentNode weatherNode = allNodes.stream()
                .filter(node -> "weather-today".equals(node.getIntentCode()))
                .findFirst()
                .orElseThrow();

        assertThat(weatherNode.getKind()).isEqualTo(IntentKind.MCP);
        assertThat(weatherNode.getMcpToolId()).isEqualTo("weather_query");
        assertThat(weatherNode.getPromptTemplate()).isNotBlank();
        assertThat(weatherNode.getParamPromptTemplate()).isNotBlank();
    }

    private static List<IntentNode> flatten(List<IntentNode> nodes) {
        List<IntentNode> result = new ArrayList<>();
        if (nodes == null) {
            return result;
        }
        for (IntentNode node : nodes) {
            result.add(node);
            result.addAll(flatten(node.getChildren()));
        }
        return result;
    }
}

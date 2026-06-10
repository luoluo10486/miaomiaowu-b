package com.personalblog.ragbackend.infra.model;

import com.personalblog.ragbackend.infra.config.AIModelProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelSelectorTest {

    @Test
    void shouldExcludeImplicitOllamaFallbackByDefault() {
        AIModelProperties properties = baseProperties();
        properties.getChat().setDefaultModel("remote-chat");
        properties.getSelection().setAllowLocalFallback(false);
        properties.getChat().setCandidates(List.of(
                candidate("remote-chat", "bailian", "qwen-plus-latest", true),
                candidate("local-chat", "ollama", "qwen3:8b-fp16", true)
        ));

        ModelSelector selector = new ModelSelector(properties, new ModelHealthStore(properties));

        List<ModelTarget> targets = selector.selectChatCandidates(false);

        assertThat(targets).extracting(ModelTarget::id).containsExactly("remote-chat");
    }

    @Test
    void shouldKeepExplicitLocalDefaultWhenLocalFallbackDisabled() {
        AIModelProperties properties = baseProperties();
        properties.getChat().setDefaultModel("local-chat");
        properties.getSelection().setAllowLocalFallback(false);
        properties.getChat().setCandidates(List.of(
                candidate("local-chat", "ollama", "qwen3:8b-fp16", true)
        ));

        ModelSelector selector = new ModelSelector(properties, new ModelHealthStore(properties));

        List<ModelTarget> targets = selector.selectChatCandidates(false);

        assertThat(targets).extracting(ModelTarget::id).containsExactly("local-chat");
    }

    @Test
    void shouldAllowOllamaFallbackWhenExplicitlyEnabled() {
        AIModelProperties properties = baseProperties();
        properties.getEmbedding().setDefaultModel("remote-embedding");
        properties.getSelection().setAllowLocalFallback(true);
        properties.getEmbedding().setCandidates(List.of(
                candidate("remote-embedding", "siliconflow", "Qwen/Qwen3-Embedding-8B", true),
                candidate("local-embedding", "ollama", "qwen3-embedding:8b-fp16", true)
        ));

        ModelSelector selector = new ModelSelector(properties, new ModelHealthStore(properties));

        List<ModelTarget> targets = selector.selectEmbeddingCandidates();

        assertThat(targets).extracting(ModelTarget::id)
                .containsExactly("remote-embedding", "local-embedding");
    }

    private AIModelProperties baseProperties() {
        AIModelProperties properties = new AIModelProperties();
        AIModelProperties.ProviderConfig bailian = new AIModelProperties.ProviderConfig();
        bailian.setUrl("https://dashscope.aliyuncs.com");
        AIModelProperties.ProviderConfig siliconflow = new AIModelProperties.ProviderConfig();
        siliconflow.setUrl("https://api.siliconflow.cn");
        AIModelProperties.ProviderConfig ollama = new AIModelProperties.ProviderConfig();
        ollama.setUrl("http://localhost:11434");
        properties.getProviders().put("bailian", bailian);
        properties.getProviders().put("siliconflow", siliconflow);
        properties.getProviders().put("ollama", ollama);
        return properties;
    }

    private AIModelProperties.ModelCandidate candidate(String id,
                                                       String provider,
                                                       String model,
                                                       boolean enabled) {
        AIModelProperties.ModelCandidate candidate = new AIModelProperties.ModelCandidate();
        candidate.setId(id);
        candidate.setProvider(provider);
        candidate.setModel(model);
        candidate.setEnabled(enabled);
        candidate.setPriority(1);
        candidate.setSupportsThinking(true);
        return candidate;
    }
}

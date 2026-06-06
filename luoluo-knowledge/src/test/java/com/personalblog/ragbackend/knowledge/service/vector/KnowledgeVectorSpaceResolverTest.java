package com.personalblog.ragbackend.knowledge.service.vector;

import com.personalblog.ragbackend.rag.config.RAGDefaultProperties;
import com.personalblog.ragbackend.rag.config.SearchChannelProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeVectorSpaceResolverTest {

    @Test
    void shouldResolveDefaultBaseToConfiguredCollection() {
        RAGDefaultProperties properties = new RAGDefaultProperties();
        properties.setCollectionName("knowledge_default_store");
        properties.setDimension(1536);

        KnowledgeVectorSpaceResolver resolver = new KnowledgeVectorSpaceResolver(
                properties,
                new SearchChannelProperties()
        );

        KnowledgeVectorSpace vectorSpace = resolver.resolve("default");

        assertThat(vectorSpace.collectionName()).isEqualTo("knowledge_default_store");
        assertThat(vectorSpace.spaceId().logicalName()).isEqualTo("default");
        assertThat(vectorSpace.spaceId().namespace()).isEqualTo("public");
    }

    @Test
    void shouldNormalizeCustomBaseCodeAndKeepLogicalCollectionNaming() {
        RAGDefaultProperties properties = new RAGDefaultProperties();
        properties.setCollectionName("default");
        properties.setDimension(1536);

        KnowledgeVectorSpaceResolver resolver = new KnowledgeVectorSpaceResolver(
                properties,
                new SearchChannelProperties()
        );

        KnowledgeVectorSpace vectorSpace = resolver.resolve(" HR Policy ");

        assertThat(vectorSpace.collectionName()).isEqualTo("kb_hr_policy");
        assertThat(vectorSpace.spaceId().logicalName()).isEqualTo("hr_policy");
        assertThat(vectorSpace.spaceId().namespace()).isEqualTo("public");
    }
}

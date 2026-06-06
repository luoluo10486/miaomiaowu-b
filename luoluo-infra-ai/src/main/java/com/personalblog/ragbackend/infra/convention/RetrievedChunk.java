package com.personalblog.ragbackend.infra.convention;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RetrievedChunk {

    private String id;
    private String text;
    private Float score;
    private Map<String, Object> metadata;

    public RetrievedChunk() {
    }

    public RetrievedChunk(String id, String text, Float score) {
        this(id, text, score, null);
    }

    public RetrievedChunk(String id, String text, Float score, Map<String, Object> metadata) {
        this.id = id;
        this.text = text;
        this.score = score;
        this.metadata = normalizeMetadata(metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = normalizeMetadata(metadata);
    }

    public static final class Builder {
        private String id;
        private String text;
        private Float score;
        private Map<String, Object> metadata;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder score(Float score) {
            this.score = score;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public RetrievedChunk build() {
            return new RetrievedChunk(id, text, score, metadata);
        }
    }

    private Map<String, Object> normalizeMetadata(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}

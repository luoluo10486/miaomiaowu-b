package com.personalblog.ragbackend.knowledge.dto.stream;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * CompletionPayload记录类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompletionPayload(String messageId, String title) {
}

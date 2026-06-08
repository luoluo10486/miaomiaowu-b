package com.personalblog.ragbackend.knowledge.dto.stream;

/**
 * 消息Delta记录类
 */
public record MessageDelta(String type, String delta) {
}

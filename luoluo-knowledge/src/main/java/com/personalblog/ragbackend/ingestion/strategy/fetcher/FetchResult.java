package com.personalblog.ragbackend.ingestion.strategy.fetcher;

/**
 * 获取结果记录类
 */
public record FetchResult(byte[] content, String mimeType, String fileName) {
}

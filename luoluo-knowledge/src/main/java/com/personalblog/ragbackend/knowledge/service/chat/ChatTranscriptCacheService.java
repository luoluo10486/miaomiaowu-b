package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.config.RagKnowledgeProcessingProperties;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatMessage;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天记录解析缓存，避免按月文档重复完整解析同一份原始聊天文件。
 */
@Slf4j
@Service
public class ChatTranscriptCacheService {

    private final RagKnowledgeProcessingProperties processingProperties;
    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();

    public ChatTranscriptCacheService(RagKnowledgeProcessingProperties processingProperties) {
        this.processingProperties = processingProperties;
    }

    public CachedTranscript getOrParse(String cacheKey,
                                       byte[] fileBytes,
                                       String fileName,
                                       ChatTranscriptParser parser) {
        if (!StringUtils.hasText(cacheKey)) {
            return parseTranscript(fileBytes, fileName, parser);
        }

        long now = System.currentTimeMillis();
        CachedEntry cached = cache.get(cacheKey);
        if (cached != null && !isExpired(cached, now)) {
            cached.lastAccessAt = now;
            return cached.transcript;
        }

        synchronized (cache) {
            now = System.currentTimeMillis();
            CachedEntry refreshed = cache.get(cacheKey);
            if (refreshed != null && !isExpired(refreshed, now)) {
                refreshed.lastAccessAt = now;
                return refreshed.transcript;
            }

            CachedTranscript parsed = parseTranscript(fileBytes, fileName, parser);
            cache.put(cacheKey, new CachedEntry(parsed, now));
            evictIfNecessary();
            return parsed;
        }
    }

    private CachedTranscript parseTranscript(byte[] fileBytes, String fileName, ChatTranscriptParser parser) {
        QqChatTranscript fullTranscript = parser.parse(fileBytes, fileName);
        Map<String, List<QqChatMessage>> monthlyMessages = new LinkedHashMap<>();
        for (QqChatMessage message : fullTranscript.messages()) {
            if (message == null || message.timestamp() == null) {
                continue;
            }
            String month = java.time.YearMonth.from(message.timestamp()).toString();
            monthlyMessages.computeIfAbsent(month, ignored -> new ArrayList<>()).add(message);
        }
        QqChatTranscript lightweightTranscript = new QqChatTranscript(
                fullTranscript.sourceFileName(),
                fullTranscript.platform(),
                fullTranscript.docType(),
                fullTranscript.groupName(),
                fullTranscript.chatType(),
                fullTranscript.exportedAt(),
                fullTranscript.messageTotal(),
                fullTranscript.rangeStart(),
                fullTranscript.rangeEnd(),
                List.of()
        );
        return new CachedTranscript(lightweightTranscript, monthlyMessages);
    }

    private boolean isExpired(CachedEntry entry, long now) {
        long ttlMillis = Math.max(60, processingProperties.getChatTranscriptCacheTtlSeconds()) * 1000L;
        return now - entry.lastAccessAt > ttlMillis;
    }

    private void evictIfNecessary() {
        int maxEntries = Math.max(1, processingProperties.getChatTranscriptCacheMaxEntries());
        if (cache.size() <= maxEntries) {
            return;
        }
        while (cache.size() > maxEntries) {
            Map.Entry<String, CachedEntry> oldest = cache.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().lastAccessAt))
                    .orElse(null);
            if (oldest == null) {
                return;
            }
            cache.remove(oldest.getKey());
        }
    }

    public record CachedTranscript(
            QqChatTranscript transcript,
            Map<String, List<QqChatMessage>> monthlyMessages
    ) {
        public CachedTranscript {
            if (monthlyMessages == null || monthlyMessages.isEmpty()) {
                monthlyMessages = Map.of();
            } else {
                Map<String, List<QqChatMessage>> copy = new LinkedHashMap<>();
                for (Map.Entry<String, List<QqChatMessage>> entry : monthlyMessages.entrySet()) {
                    copy.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
                }
                monthlyMessages = Collections.unmodifiableMap(copy);
            }
        }

        public List<QqChatMessage> monthlyMessages(String bucketMonth) {
            if (!StringUtils.hasText(bucketMonth)) {
                return List.of();
            }
            List<QqChatMessage> messages = monthlyMessages.get(bucketMonth);
            return messages == null ? List.of() : List.copyOf(messages);
        }
    }

    private static final class CachedEntry {
        private final CachedTranscript transcript;
        private volatile long lastAccessAt;

        private CachedEntry(CachedTranscript transcript, long lastAccessAt) {
            this.transcript = transcript;
            this.lastAccessAt = lastAccessAt;
        }
    }
}

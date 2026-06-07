package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.dto.chat.QqChatMessage;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import com.personalblog.ragbackend.knowledge.dto.document.DocumentChunk;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class ChatTranscriptChunkService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<DocumentChunk> chunkTranscript(QqChatTranscript transcript,
                                               String bucketMonth,
                                               ChatChunkingOptions options) {
        if (transcript == null || transcript.messages().isEmpty() || !StringUtils.hasText(bucketMonth)) {
            return List.of();
        }

        YearMonth month = YearMonth.parse(bucketMonth);
        List<QqChatMessage> monthlyMessages = transcript.messages().stream()
                .filter(message -> message != null && message.timestamp() != null)
                .filter(message -> YearMonth.from(message.timestamp()).equals(month))
                .toList();
        if (monthlyMessages.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < monthlyMessages.size() && chunks.size() < options.maxChunkCount()) {
            List<QqChatMessage> window = new ArrayList<>();
            int charCount = 0;
            int cursor = start;
            while (cursor < monthlyMessages.size()) {
                QqChatMessage candidate = monthlyMessages.get(cursor);
                String candidateLine = formatMessage(candidate);
                int projectedCharCount = charCount + (window.isEmpty() ? 0 : 1) + candidateLine.length();
                boolean exceedsMessageLimit = window.size() >= options.maxMessages();
                boolean exceedsCharLimit = !window.isEmpty()
                        && projectedCharCount > options.maxChars()
                        && window.size() >= options.minMessages();
                boolean exceedsTarget = !window.isEmpty()
                        && projectedCharCount > options.targetChars()
                        && window.size() >= options.minMessages();
                boolean exceedsGap = !window.isEmpty()
                        && gapMinutes(window.get(window.size() - 1).timestamp(), candidate.timestamp()) > options.splitGapMinutes()
                        && window.size() >= options.minMessages();
                if (exceedsMessageLimit || exceedsCharLimit || exceedsTarget || exceedsGap) {
                    break;
                }

                window.add(candidate);
                charCount = projectedCharCount;
                cursor++;
            }

            if (window.isEmpty()) {
                window.add(monthlyMessages.get(start));
                cursor = start + 1;
            }

            chunks.add(toChunk(transcript, bucketMonth, chunks.size() + 1, window, start > 0));
            if (cursor >= monthlyMessages.size()) {
                break;
            }

            int nextStart = Math.max(cursor - options.overlapMessages(), start + 1);
            start = nextStart;
        }

        return chunks;
    }

    private DocumentChunk toChunk(QqChatTranscript transcript,
                                  String bucketMonth,
                                  int chunkIndex,
                                  List<QqChatMessage> window,
                                  boolean overlapFromPrevious) {
        QqChatMessage first = window.get(0);
        QqChatMessage last = window.get(window.size() - 1);
        LinkedHashSet<String> speakerSet = new LinkedHashSet<>();
        List<String> lines = new ArrayList<>(window.size());
        for (QqChatMessage message : window) {
            if (StringUtils.hasText(message.speakerName())) {
                speakerSet.add(message.speakerName());
            }
            lines.add(formatMessage(message));
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("docType", "chat_qq_group");
        metadata.put("groupName", transcript.groupName());
        metadata.put("bucketMonth", bucketMonth);
        metadata.put("startTime", formatDateTime(first.timestamp()));
        metadata.put("endTime", formatDateTime(last.timestamp()));
        metadata.put("speakerSet", List.copyOf(speakerSet));
        metadata.put("messageStartIndex", first.messageIndex());
        metadata.put("messageEndIndex", last.messageIndex());
        metadata.put("messageCount", window.size());
        metadata.put("lineStart", first.lineStart());
        metadata.put("lineEnd", last.lineEnd());
        metadata.put("sourceFile", transcript.sourceFileName());

        return new DocumentChunk(
                chunkIndex,
                sectionTitle(transcript.groupName(), bucketMonth),
                String.join("\n", lines),
                lines.stream().mapToInt(String::length).sum() + Math.max(0, lines.size() - 1),
                overlapFromPrevious,
                metadata
        );
    }

    private String formatMessage(QqChatMessage message) {
        String content = message.content() == null ? "" : message.content().replace("\r\n", "\n").replace('\r', '\n');
        content = content.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return "[" + formatDateTime(message.timestamp()) + "] " + message.speakerName() + ": " + content;
    }

    private long gapMinutes(LocalDateTime previous, LocalDateTime current) {
        if (previous == null || current == null) {
            return 0L;
        }
        return Math.abs(Duration.between(previous, current).toMinutes());
    }

    private String sectionTitle(String groupName, String bucketMonth) {
        if (!StringUtils.hasText(groupName)) {
            return bucketMonth;
        }
        return groupName + "#" + bucketMonth;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }
}

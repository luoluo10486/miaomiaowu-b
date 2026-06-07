package com.personalblog.ragbackend.rag.core.retrieve.postprocessor;

import com.personalblog.ragbackend.infra.convention.RetrievedChunk;
import com.personalblog.ragbackend.rag.core.retrieve.channel.SearchChannelResult;
import com.personalblog.ragbackend.rag.core.retrieve.channel.SearchContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChatTranscriptBoostPostProcessor implements SearchResultPostProcessor {
    private static final String CHAT_DOC_TYPE = "chat_qq_group";
    private static final Pattern DAY_PATTERN = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");
    private static final Pattern MONTH_PATTERN = Pattern.compile("\\b(20\\d{2}-\\d{2})\\b");
    private static final Pattern MONTH_CN_PATTERN = Pattern.compile("(20\\d{2})\\s*年\\s*(\\d{1,2})\\s*月");

    @Override
    public String getName() {
        return "ChatTranscriptBoost";
    }

    @Override
    public int getOrder() {
        return 5;
    }

    @Override
    public List<RetrievedChunk> process(List<RetrievedChunk> chunks, List<SearchChannelResult> results, SearchContext context) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        String question = context == null ? "" : context.getMainQuestion();
        if (!StringUtils.hasText(question)) {
            return chunks;
        }

        String day = extractFirst(DAY_PATTERN, question);
        String month = resolveMonth(question);
        List<RetrievedChunk> boosted = new ArrayList<>(chunks.size());
        boolean changed = false;
        for (RetrievedChunk chunk : chunks) {
            if (chunk == null) {
                continue;
            }
            Map<String, Object> metadata = chunk.getMetadata();
            if (!isChatChunk(metadata)) {
                boosted.add(chunk);
                continue;
            }
            float nextScore = chunk.getScore() == null ? 0F : chunk.getScore();
            float boost = 0F;
            if (StringUtils.hasText(day) && dayMatches(metadata, day)) {
                boost += 0.12F;
            }
            if (StringUtils.hasText(month) && monthMatches(metadata, month)) {
                boost += 0.08F;
            }
            if (speakerMatches(metadata, question)) {
                boost += 0.10F;
            }
            if (boost > 0F) {
                nextScore += boost;
                changed = true;
            }
            chunk.setScore(nextScore);
            boosted.add(chunk);
        }

        if (!changed) {
            return boosted;
        }
        boosted.sort(Comparator.comparingDouble((RetrievedChunk chunk) -> chunk.getScore() == null ? 0D : chunk.getScore()).reversed());
        return boosted;
    }

    private boolean isChatChunk(Map<String, Object> metadata) {
        return metadata != null && CHAT_DOC_TYPE.equals(String.valueOf(metadata.get("docType")));
    }

    private boolean dayMatches(Map<String, Object> metadata, String day) {
        String startTime = stringValue(metadata, "startTime");
        String endTime = stringValue(metadata, "endTime");
        return (StringUtils.hasText(startTime) && startTime.startsWith(day))
                || (StringUtils.hasText(endTime) && endTime.startsWith(day));
    }

    private boolean monthMatches(Map<String, Object> metadata, String month) {
        String bucketMonth = stringValue(metadata, "bucketMonth");
        if (StringUtils.hasText(bucketMonth) && month.equals(bucketMonth)) {
            return true;
        }
        String startTime = stringValue(metadata, "startTime");
        String endTime = stringValue(metadata, "endTime");
        return (StringUtils.hasText(startTime) && startTime.startsWith(month))
                || (StringUtils.hasText(endTime) && endTime.startsWith(month));
    }

    private boolean speakerMatches(Map<String, Object> metadata, String question) {
        Object speakerSet = metadata == null ? null : metadata.get("speakerSet");
        if (!(speakerSet instanceof Iterable<?> iterable)) {
            return false;
        }
        for (Object speaker : iterable) {
            if (speaker == null) {
                continue;
            }
            String name = String.valueOf(speaker).trim();
            if (StringUtils.hasText(name) && question.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private String extractFirst(Pattern pattern, String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String resolveMonth(String text) {
        String isoMonth = extractFirst(MONTH_PATTERN, text);
        if (StringUtils.hasText(isoMonth)) {
            return isoMonth;
        }
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = MONTH_CN_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + "-" + String.format("%02d", Integer.parseInt(matcher.group(2)));
    }

    private String stringValue(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key) || metadata.get(key) == null) {
            return null;
        }
        String value = String.valueOf(metadata.get(key)).trim();
        return value.isEmpty() ? null : value;
    }
}

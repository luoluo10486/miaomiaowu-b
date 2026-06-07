package com.personalblog.ragbackend.knowledge.service.chat;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChatTranscriptParserRegistry {
    private final Map<String, ChatTranscriptParser> parsersByPlatform;
    private final Map<String, ChatTranscriptParser> parsersByDocType;

    public ChatTranscriptParserRegistry(List<ChatTranscriptParser> parsers) {
        this.parsersByPlatform = indexByPlatform(parsers);
        this.parsersByDocType = indexByDocType(parsers);
    }

    public ChatTranscriptParser requireByPlatform(String platform) {
        ChatTranscriptParser parser = parsersByPlatform.get(normalize(platform));
        if (parser == null) {
            throw new IllegalArgumentException("unsupported chat transcript platform: " + platform);
        }
        return parser;
    }

    public ChatTranscriptParser requireByDocType(String docType) {
        ChatTranscriptParser parser = parsersByDocType.get(normalize(docType));
        if (parser == null) {
            throw new IllegalArgumentException("unsupported chat transcript docType: " + docType);
        }
        return parser;
    }

    private Map<String, ChatTranscriptParser> indexByPlatform(List<ChatTranscriptParser> parsers) {
        LinkedHashMap<String, ChatTranscriptParser> result = new LinkedHashMap<>();
        if (parsers == null) {
            return result;
        }
        for (ChatTranscriptParser parser : parsers) {
            if (parser == null) {
                continue;
            }
            String key = normalize(parser.platform());
            if (!StringUtils.hasText(key) || result.containsKey(key)) {
                continue;
            }
            result.put(key, parser);
        }
        return Map.copyOf(result);
    }

    private Map<String, ChatTranscriptParser> indexByDocType(List<ChatTranscriptParser> parsers) {
        LinkedHashMap<String, ChatTranscriptParser> result = new LinkedHashMap<>();
        if (parsers == null) {
            return result;
        }
        for (ChatTranscriptParser parser : parsers) {
            if (parser == null) {
                continue;
            }
            String key = normalize(parser.docType());
            if (!StringUtils.hasText(key) || result.containsKey(key)) {
                continue;
            }
            result.put(key, parser);
        }
        return Map.copyOf(result);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }
}

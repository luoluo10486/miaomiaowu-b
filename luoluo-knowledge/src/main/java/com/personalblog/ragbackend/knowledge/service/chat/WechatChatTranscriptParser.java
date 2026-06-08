package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.dto.chat.QqChatMessage;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wechat对话Transcript解析器
 */
@Service
public class WechatChatTranscriptParser implements ChatTranscriptParser {
    public static final String PLATFORM = "wechat";
    public static final String DOC_TYPE = "chat_wechat_group";

    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final DateTimeFormatter DATE_TIME_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String HEADER_GROUP_NAME = "\u804a\u5929\u8bb0\u5f55:";
    private static final String HEADER_CHAT_TYPE = "\u7c7b\u578b:";
    private static final String HEADER_RANGE = "\u65f6\u95f4\u8303\u56f4:";
    private static final String HEADER_EXPORTED_AT = "\u5bfc\u51fa\u65f6\u95f4:";
    private static final String HEADER_MESSAGE_TOTAL = "\u6d88\u606f\u6570\u91cf:";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(?::\\d{2})?)]\\s+(.+?):\\s*(.*)$");
    private static final Pattern SYSTEM_MESSAGE_PATTERN = Pattern.compile("^\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(?::\\d{2})?)]\\s+\\[(.+?)]\\s*(.*)$");

    @Override
    public String platform() {
        return PLATFORM;
    }

    @Override
    public String docType() {
        return DOC_TYPE;
    }

    @Override
    public QqChatTranscript parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("chat transcript file is required");
        }
        try {
            String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "wechat-chat.txt";
            return parse(file.getBytes(), fileName);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read chat transcript file", exception);
        }
    }

    public QqChatTranscript parse(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("chat transcript bytes are required");
        }

        String content = decode(bytes);
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);

        Map<String, String> header = new LinkedHashMap<>();
        List<QqChatMessage> messages = new ArrayList<>();
        int index = 0;
        while (index < lines.length) {
            String trimmed = lines[index].trim();
            if (trimmed.startsWith(HEADER_GROUP_NAME)) {
                header.put("groupName", trimmed.substring(HEADER_GROUP_NAME.length()).trim());
            } else if (trimmed.startsWith(HEADER_CHAT_TYPE)) {
                header.put("chatType", trimmed.substring(HEADER_CHAT_TYPE.length()).trim());
            } else if (trimmed.startsWith(HEADER_RANGE)) {
                header.put("range", trimmed.substring(HEADER_RANGE.length()).trim());
            } else if (trimmed.startsWith(HEADER_EXPORTED_AT)) {
                header.put("exportedAt", trimmed.substring(HEADER_EXPORTED_AT.length()).trim());
            } else if (trimmed.startsWith(HEADER_MESSAGE_TOTAL)) {
                header.put("messageTotal", trimmed.substring(HEADER_MESSAGE_TOTAL.length()).trim());
            }

            MessageHeader headerMatch = matchMessageHeader(lines[index]);
            if (headerMatch == null) {
                index++;
                continue;
            }

            int cursor = index + 1;
            List<String> contentLines = new ArrayList<>();
            contentLines.add(headerMatch.content());
            int lastContentLine = index;
            while (cursor < lines.length && matchMessageHeader(lines[cursor]) == null) {
                contentLines.add(lines[cursor]);
                if (StringUtils.hasText(lines[cursor])) {
                    lastContentLine = cursor;
                }
                cursor++;
            }

            messages.add(new QqChatMessage(
                    headerMatch.speakerTag(),
                    headerMatch.speakerName(),
                    headerMatch.timestamp(),
                    normalizeMessageContent(contentLines),
                    messages.size() + 1,
                    index + 1,
                    lastContentLine + 1
            ));
            index = cursor;
        }

        LocalDateTime exportedAt = parseDateTime(header.get("exportedAt"));
        Integer messageTotal = parseInteger(header.get("messageTotal"));
        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;
        if (StringUtils.hasText(header.get("range")) && header.get("range").contains(" - ")) {
            String[] rangeParts = header.get("range").split("\\s+-\\s+", 2);
            rangeStart = rangeParts.length > 0 ? parseDateTime(rangeParts[0]) : null;
            rangeEnd = rangeParts.length > 1 ? parseDateTime(rangeParts[1]) : null;
        }

        return new QqChatTranscript(
                fileName,
                PLATFORM,
                DOC_TYPE,
                header.getOrDefault("groupName", ""),
                header.getOrDefault("chatType", ""),
                exportedAt,
                messageTotal,
                rangeStart,
                rangeEnd,
                messages
        );
    }

    private MessageHeader matchMessageHeader(String line) {
        Matcher matcher = MESSAGE_PATTERN.matcher(line == null ? "" : line.trim());
        if (matcher.matches()) {
            LocalDateTime timestamp = parseDateTimeRequired(matcher.group(1));
            String speakerName = matcher.group(2).trim();
            return new MessageHeader(speakerName, speakerName, timestamp, matcher.group(3).trim());
        }

        Matcher systemMatcher = SYSTEM_MESSAGE_PATTERN.matcher(line == null ? "" : line.trim());
        if (!systemMatcher.matches()) {
            return null;
        }
        LocalDateTime timestamp = parseDateTimeRequired(systemMatcher.group(1));
        String speakerTag = systemMatcher.group(2).trim();
        return new MessageHeader(speakerTag, "[" + speakerTag + "]", timestamp, systemMatcher.group(3).trim());
    }

    private String normalizeMessageContent(List<String> contentLines) {
        List<String> normalized = new ArrayList<>();
        if (contentLines != null) {
            for (String line : contentLines) {
                normalized.add(line == null ? "" : line.stripTrailing());
            }
        }
        while (!normalized.isEmpty() && normalized.get(normalized.size() - 1).isBlank()) {
            normalized.remove(normalized.size() - 1);
        }
        return String.join("\n", normalized).trim();
    }

    private String decode(byte[] bytes) {
        if (hasUtf8Bom(bytes)) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        String utf8 = tryDecode(bytes, StandardCharsets.UTF_8);
        if (looksLikeWechatTranscript(utf8)) {
            return utf8;
        }

        String gb18030 = tryDecode(bytes, GB18030);
        if (looksLikeWechatTranscript(gb18030)) {
            return gb18030;
        }
        return utf8;
    }

    private String tryDecode(byte[] bytes, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ignored) {
            return new String(bytes, charset);
        }
    }

    private boolean looksLikeWechatTranscript(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        return content.contains(HEADER_GROUP_NAME)
                || content.contains(HEADER_MESSAGE_TOTAL)
                || MESSAGE_PATTERN.matcher(content.lines().findFirst().orElse("")).matches();
    }

    private boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF;
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_SECONDS);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value.trim(), DATE_TIME_MINUTES);
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private LocalDateTime parseDateTimeRequired(String value) {
        LocalDateTime dateTime = parseDateTime(value);
        if (dateTime == null) {
            throw new IllegalArgumentException("wechat transcript datetime is invalid: " + value);
        }
        return dateTime;
    }

    private record MessageHeader(String speakerTag, String speakerName, LocalDateTime timestamp, String content) {
    }
}

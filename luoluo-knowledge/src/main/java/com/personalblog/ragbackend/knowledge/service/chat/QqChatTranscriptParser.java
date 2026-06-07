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

@Service
public class QqChatTranscriptParser implements ChatTranscriptParser {
    public static final String PLATFORM = "qq";
    public static final String DOC_TYPE = "chat_qq_group";
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\\[(.*?)]\\s*(.*?):\\s*$");
    private static final String HEADER_GROUP_NAME = "\u804a\u5929\u540d\u79f0:";
    private static final String HEADER_CHAT_TYPE = "\u804a\u5929\u7c7b\u578b:";
    private static final String HEADER_EXPORTED_AT = "\u5bfc\u51fa\u65f6\u95f4:";
    private static final String HEADER_MESSAGE_TOTAL = "\u6d88\u606f\u603b\u6570:";
    private static final String HEADER_RANGE = "\u65f6\u95f4\u8303\u56f4:";
    private static final String HEADER_TIME = "\u65f6\u95f4:";
    private static final String HEADER_CONTENT = "\u5185\u5bb9:";
    private static final String TRANSCRIPT_TITLE = "QQ\u804a\u5929\u8bb0\u5f55\u5bfc\u51fa\u6587\u4ef6";

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
            String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "qq-chat.txt";
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
            } else if (trimmed.startsWith(HEADER_EXPORTED_AT)) {
                header.put("exportedAt", trimmed.substring(HEADER_EXPORTED_AT.length()).trim());
            } else if (trimmed.startsWith(HEADER_MESSAGE_TOTAL)) {
                header.put("messageTotal", trimmed.substring(HEADER_MESSAGE_TOTAL.length()).trim());
            } else if (trimmed.startsWith(HEADER_RANGE)) {
                header.put("range", trimmed.substring(HEADER_RANGE.length()).trim());
            }

            if (isMessageHeader(lines, index)) {
                MessageParseResult result = parseMessage(lines, index, messages.size() + 1);
                messages.add(result.message());
                index = result.nextIndex();
                continue;
            }
            index++;
        }

        LocalDateTime exportedAt = parseDateTime(header.get("exportedAt"));
        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;
        if (StringUtils.hasText(header.get("range"))) {
            String[] range = header.get("range").split("\\s+-\\s+", 2);
            rangeStart = range.length > 0 ? parseDateTime(range[0]) : null;
            rangeEnd = range.length > 1 ? parseDateTime(range[1]) : null;
        }

        Integer messageTotal = null;
        if (StringUtils.hasText(header.get("messageTotal"))) {
            try {
                messageTotal = Integer.parseInt(header.get("messageTotal"));
            } catch (NumberFormatException ignored) {
                messageTotal = null;
            }
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

    private MessageParseResult parseMessage(String[] lines, int startIndex, int messageIndex) {
        Matcher matcher = HEADER_PATTERN.matcher(lines[startIndex].trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("chat transcript message header is invalid at line " + (startIndex + 1));
        }
        String speakerTag = matcher.group(1).trim();
        String speakerName = matcher.group(2).trim();

        int cursor = startIndex + 1;
        while (cursor < lines.length && lines[cursor].trim().isEmpty()) {
            cursor++;
        }
        if (cursor >= lines.length || !lines[cursor].trim().startsWith(HEADER_TIME)) {
            throw new IllegalArgumentException("chat transcript message time line is invalid at line " + (startIndex + 1));
        }
        LocalDateTime timestamp = parseDateTimeRequired(lines[cursor].trim().substring(HEADER_TIME.length()).trim(), startIndex + 1);

        cursor++;
        while (cursor < lines.length && lines[cursor].trim().isEmpty()) {
            cursor++;
        }
        if (cursor >= lines.length || !lines[cursor].trim().startsWith(HEADER_CONTENT)) {
            throw new IllegalArgumentException("chat transcript message content line is invalid at line " + (startIndex + 1));
        }

        String firstContentLine = lines[cursor].trim().substring(HEADER_CONTENT.length()).trim();
        List<String> contentLines = new ArrayList<>();
        contentLines.add(firstContentLine);
        int lastContentLine = cursor;
        cursor++;
        while (cursor < lines.length) {
            if (isMessageHeader(lines, cursor)) {
                break;
            }
            contentLines.add(lines[cursor]);
            if (StringUtils.hasText(lines[cursor])) {
                lastContentLine = cursor;
            }
            cursor++;
        }

        String content = normalizeMessageContent(contentLines);
        return new MessageParseResult(
                new QqChatMessage(
                        speakerTag,
                        speakerName,
                        timestamp,
                        content,
                        messageIndex,
                        startIndex + 1,
                        lastContentLine + 1
                ),
                cursor
        );
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

    private boolean isMessageHeader(String[] lines, int index) {
        if (lines == null || index < 0 || index >= lines.length) {
            return false;
        }
        String trimmed = lines[index].trim();
        if (!HEADER_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        int next = index + 1;
        while (next < lines.length && lines[next].trim().isEmpty()) {
            next++;
        }
        return next < lines.length && lines[next].trim().startsWith(HEADER_TIME);
    }

    private String decode(byte[] bytes) {
        if (hasUtf8Bom(bytes)) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        String utf8 = tryDecode(bytes, StandardCharsets.UTF_8);
        if (looksLikeQqTranscript(utf8)) {
            return utf8;
        }

        String gb18030 = tryDecode(bytes, GB18030);
        if (looksLikeQqTranscript(gb18030)) {
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

    private boolean looksLikeQqTranscript(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        return content.contains(TRANSCRIPT_TITLE)
                || content.contains(HEADER_GROUP_NAME)
                || content.contains(HEADER_MESSAGE_TOTAL)
                || content.contains(HEADER_RANGE);
    }

    private boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF;
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateTimeRequired(String value, int lineNumber) {
        LocalDateTime dateTime = parseDateTime(value);
        if (dateTime == null) {
            throw new IllegalArgumentException("chat transcript datetime is invalid at line " + lineNumber);
        }
        return dateTime;
    }

    private record MessageParseResult(QqChatMessage message, int nextIndex) {
    }
}

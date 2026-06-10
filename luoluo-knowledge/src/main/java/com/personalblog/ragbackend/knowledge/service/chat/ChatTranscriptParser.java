package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.dto.chat.QqChatMessage;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对话Transcript解析器
 */
public interface ChatTranscriptParser {

    String platform();

    String docType();

    default ChatTranscriptInspection inspect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("chat transcript file is required");
        }
        try {
            String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : platform() + "-chat.txt";
            return inspect(file.getBytes(), fileName);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read chat transcript file", exception);
        }
    }

    default QqChatTranscript parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("chat transcript file is required");
        }
        try {
            String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : platform() + "-chat.txt";
            return parse(file.getBytes(), fileName);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read chat transcript file", exception);
        }
    }

    default ChatTranscriptInspection inspect(byte[] bytes, String fileName) {
        QqChatTranscript transcript = parse(bytes, fileName);
        Map<String, Integer> monthMessageCounts = new LinkedHashMap<>();
        for (QqChatMessage message : transcript.messages()) {
            if (message == null || message.timestamp() == null) {
                continue;
            }
            String month = YearMonth.from(message.timestamp()).toString();
            monthMessageCounts.merge(month, 1, Integer::sum);
        }
        return new ChatTranscriptInspection(
                transcript.sourceFileName(),
                transcript.platform(),
                transcript.docType(),
                transcript.groupName(),
                transcript.chatType(),
                transcript.exportedAt(),
                transcript.messageTotal(),
                transcript.rangeStart(),
                transcript.rangeEnd(),
                monthMessageCounts
        );
    }

    QqChatTranscript parse(byte[] bytes, String fileName);
}

package com.personalblog.ragbackend.knowledge.service.chat;

import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import org.springframework.web.multipart.MultipartFile;

public interface ChatTranscriptParser {

    String platform();

    String docType();

    QqChatTranscript parse(MultipartFile file);
}

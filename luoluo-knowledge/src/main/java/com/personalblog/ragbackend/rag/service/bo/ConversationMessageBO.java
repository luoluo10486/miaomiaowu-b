package com.personalblog.ragbackend.rag.service.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话消息业务对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessageBO {
    private String conversationId;
    private String userId;
    private String role;
    private String content;
    private String thinkingContent;
    private Integer thinkingDuration;
}

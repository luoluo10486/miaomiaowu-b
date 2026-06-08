package com.personalblog.ragbackend.rag.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 会话创建请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationCreateRequest {
    private String conversationId;
    private String userId;
    private String question;
    private Date lastTime;
}

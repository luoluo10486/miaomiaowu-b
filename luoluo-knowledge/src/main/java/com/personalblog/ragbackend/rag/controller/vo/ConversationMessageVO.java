package com.personalblog.ragbackend.rag.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 会话消息视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessageVO {
    private String id;
    private String conversationId;
    private String role;
    private String content;
    private String thinkingContent;
    private Integer thinkingDuration;
    private Integer vote;
    private Date createTime;
}

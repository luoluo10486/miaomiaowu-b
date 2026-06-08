package com.personalblog.ragbackend.rag.controller.request;

import lombok.Data;

/**
 * 消息Feedback请求对象
 */
@Data
public class MessageFeedbackRequest {
    private Integer vote;
    private String reason;
    private String comment;
}

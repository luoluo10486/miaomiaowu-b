package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

/**
 * 对话导入请求对象
 */
@Data
public class ChatImportRequest {
    private Boolean autoStart;
    private String splitBy;
    private Integer minMessages;
    private Integer maxMessages;
    private Integer overlapMessages;
    private Integer targetChars;
    private Integer maxChars;
    private Integer splitGapMinutes;
}

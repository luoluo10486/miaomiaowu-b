package com.personalblog.ragbackend.knowledge.controller.request;

import lombok.Data;

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

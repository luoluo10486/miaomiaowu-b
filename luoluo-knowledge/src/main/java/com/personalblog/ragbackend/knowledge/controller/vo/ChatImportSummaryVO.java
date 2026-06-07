package com.personalblog.ragbackend.knowledge.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChatImportSummaryVO {
    private String groupName;
    private String sourceFileUrl;
    private Integer createdDocCount;
    private List<String> createdDocIds;
    private List<String> months;
}

package com.personalblog.ragbackend.knowledge.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 对话导入汇总视图对象
 */
@Data
@AllArgsConstructor
public class ChatImportSummaryVO {
    private String groupName;
    private String sourceFileUrl;
    private Integer createdDocCount;
    private List<String> createdDocIds;
    private List<String> months;
}

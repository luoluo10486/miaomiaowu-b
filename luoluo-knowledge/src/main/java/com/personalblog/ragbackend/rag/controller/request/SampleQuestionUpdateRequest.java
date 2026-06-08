package com.personalblog.ragbackend.rag.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 样例问题更新请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleQuestionUpdateRequest {
    private String title;
    private String description;
    private String question;
}

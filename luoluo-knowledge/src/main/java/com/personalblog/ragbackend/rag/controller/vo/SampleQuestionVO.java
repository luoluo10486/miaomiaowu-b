package com.personalblog.ragbackend.rag.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 样例问题视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleQuestionVO {
    private String id;
    private String title;
    private String description;
    private String question;
    private Date createTime;
    private Date updateTime;
}

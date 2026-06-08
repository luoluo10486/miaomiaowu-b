package com.personalblog.ragbackend.knowledge.controller.vo;

import lombok.Data;

import java.util.Date;

/**
 * 知识Base视图对象
 */
@Data
public class KnowledgeBaseVO {
    private String id;
    private String name;
    private String description;
    private String embeddingModel;
    private String collectionName;
    private String visibility;
    private String allowedRoles;
    private String status;
    private String ownerUserId;
    private Long documentCount;
    private String createdBy;
    private Date createTime;
    private Date updateTime;
}

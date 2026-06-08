package com.personalblog.ragbackend.knowledge.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.personalblog.ragbackend.knowledge.dao.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识分块数据对象
 */
@TableName(value = "t_knowledge_chunk", autoResultMap = true)
@Data
public class KnowledgeChunkDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("kb_id")
    private Long kbId;
    @TableField("doc_id")
    private Long docId;
    @TableField("chunk_index")
    private Integer chunkIndex;
    @TableField("content")
    private String content;
    @TableField("content_hash")
    private String contentHash;
    @TableField("char_count")
    private Integer charCount;
    @TableField("token_count")
    private Integer tokenCount;
    @TableField("enabled")
    private Integer enabled;
    @TableField(exist = false)
    private String vectorId;
    @TableField(value = "metadata", typeHandler = JsonbTypeHandler.class)
    private String metadata;
    @TableField("created_by")
    private Long createdBy;
    @TableField("updated_by")
    private Long updatedBy;
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
    @TableField("create_time")
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;
}

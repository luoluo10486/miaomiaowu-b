package com.personalblog.ragbackend.knowledge.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识文档分块日志数据对象
 */
@TableName("t_knowledge_document_chunk_log")
@Data
public class KnowledgeDocumentChunkLogDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("doc_id")
    private Long docId;
    @TableField("pipeline_id")
    private Long pipelineId;
    @TableField("process_mode")
    private String processMode;
    @TableField("chunk_strategy")
    private String chunkStrategy;
    @TableField("chunk_count")
    private Integer chunkCount;
    @TableField("status")
    private String status;
    @TableField("error_message")
    private String errorMessage;
    @TableField("start_time")
    private LocalDateTime startedAt;
    @TableField("end_time")
    private LocalDateTime endedAt;
    @TableField("extract_duration")
    private Long extractDuration;
    @TableField("chunk_duration")
    private Long chunkDuration;
    @TableField("embed_duration")
    private Long embedDuration;
    @TableField("persist_duration")
    private Long persistDuration;
    @TableField("total_duration")
    private Long totalDuration;
    @TableField("create_time")
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;
}

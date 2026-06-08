package com.personalblog.ragbackend.ingestion.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.personalblog.ragbackend.knowledge.dao.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Ingestion任务数据对象
 */
@TableName(value = "t_ingestion_task", autoResultMap = true)
@Data
public class IngestionTaskDO {
    @TableId(value = "id", type = IdType.AUTO)
    public Long id;
    @TableField("pipeline_id")
    public Long pipelineId;
    @TableField("kb_id")
    public Long kbId;
    @TableField("doc_id")
    public Long docId;
    @TableField("source_type")
    public String sourceType;
    @TableField("source_location")
    public String sourceLocation;
    @TableField("source_file_name")
    public String sourceFileName;
    @TableField("status")
    public String status;
    @TableField("chunk_count")
    public Integer chunkCount;
    @TableField("error_message")
    public String errorMessage;
    @TableField(value = "logs_json", typeHandler = JsonbTypeHandler.class)
    public String logsJson;
    @TableField(value = "metadata_json", typeHandler = JsonbTypeHandler.class)
    public String metadataJson;
    @TableField("started_at")
    public LocalDateTime startedAt;
    @TableField("completed_at")
    public LocalDateTime completedAt;
    @TableField("created_by")
    public Long createdBy;
    @TableField("updated_by")
    public Long updatedBy;
    @TableLogic
    @TableField("deleted")
    public Integer deleted;
    @TableField("create_time")
    public LocalDateTime createdAt;
    @TableField("update_time")
    public LocalDateTime updatedAt;
}

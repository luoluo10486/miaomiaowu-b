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
 * Ingestion流程节点数据对象
 */
@TableName(value = "t_ingestion_pipeline_node", autoResultMap = true)
@Data
public class IngestionPipelineNodeDO {
    @TableId(value = "id", type = IdType.AUTO)
    public Long id;
    @TableField("pipeline_id")
    public Long pipelineId;
    @TableField("node_id")
    public String nodeId;
    @TableField("node_type")
    public String nodeType;
    @TableField("next_node_id")
    public String nextNodeId;
    @TableField(value = "settings_json", typeHandler = JsonbTypeHandler.class)
    public String settingsJson;
    @TableField(value = "condition_json", typeHandler = JsonbTypeHandler.class)
    public String conditionJson;
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

package com.personalblog.ragbackend.rag.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_rag_question_quota_audit")
public class RagQuestionQuotaAuditEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("action_type")
    private String actionType;

    @TableField("actor_user_id")
    private Long actorUserId;

    @TableField("actor_username")
    private String actorUsername;

    @TableField("actor_role")
    private String actorRole;

    @TableField("target_user_id")
    private Long targetUserId;

    @TableField("target_username")
    private String targetUsername;

    @TableField("old_daily_limit")
    private Integer oldDailyLimit;

    @TableField("new_daily_limit")
    private Integer newDailyLimit;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createdAt;

    @TableField("update_time")
    private LocalDateTime updatedAt;
}

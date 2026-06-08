package com.personalblog.ragbackend.common.auth.dto;

/**
 * 校验验证码IssueCommand记录类
 */
public record VerifyCodeIssueCommand(
        String namespace,
        String bizType,
        String bizId,
        String subjectType,
        Long subjectId,
        String targetType,
        String targetValue,
        String channel,
        String templateId,
        String provider,
        String requestId,
        String verifyCode,
        long ttlSeconds,
        String remark
) {
}

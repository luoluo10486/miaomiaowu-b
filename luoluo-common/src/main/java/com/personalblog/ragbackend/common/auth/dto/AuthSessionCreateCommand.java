package com.personalblog.ragbackend.common.auth.dto;

/**
 * 认证会话创建Command记录类
 */
public record AuthSessionCreateCommand(
        Long subjectId,
        String subjectType,
        String loginType,
        long ttlSeconds,
        String deviceType,
        String clientIp
) {
}

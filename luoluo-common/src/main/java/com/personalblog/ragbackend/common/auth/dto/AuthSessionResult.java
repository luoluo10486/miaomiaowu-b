package com.personalblog.ragbackend.common.auth.dto;

import java.time.LocalDateTime;

/**
 * 认证会话结果记录类
 */
public record AuthSessionResult(
        Long sessionId,
        String token,
        LocalDateTime expiresAt
) {
}

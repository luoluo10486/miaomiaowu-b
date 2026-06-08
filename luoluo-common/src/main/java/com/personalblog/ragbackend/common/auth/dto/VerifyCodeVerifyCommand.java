package com.personalblog.ragbackend.common.auth.dto;

/**
 * 校验验证码校验Command记录类
 */
public record VerifyCodeVerifyCommand(
        String namespace,
        String bizType,
        String targetType,
        String targetValue,
        String inputCode,
        boolean allowMockCode,
        String mockCode
) {
}

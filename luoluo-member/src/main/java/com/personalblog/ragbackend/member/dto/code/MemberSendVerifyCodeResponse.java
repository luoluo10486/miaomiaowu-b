package com.personalblog.ragbackend.member.dto.code;

/**
 * 会员Send校验验证码响应对象
 */
public record MemberSendVerifyCodeResponse(
        String requestId,
        String grantType,
        String target,
        long expiresIn,
        String issuedCode
) {
}

package com.personalblog.ragbackend.member.dto.auth;

/**
 * 会员登录响应对象
 */
public record MemberLoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String grantType,
        MemberUserSummary user
) {
}

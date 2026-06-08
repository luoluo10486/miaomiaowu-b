package com.personalblog.ragbackend.member.dto.auth;

/**
 * 会员用户汇总
 */
public record MemberUserSummary(
        Long id,
        String username,
        String displayName,
        String phone,
        String email,
        String userType
) {
}

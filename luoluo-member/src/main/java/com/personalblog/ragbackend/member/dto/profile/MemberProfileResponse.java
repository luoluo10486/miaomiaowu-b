package com.personalblog.ragbackend.member.dto.profile;

/**
 * 会员资料响应对象
 */
public record MemberProfileResponse(
        Long id,
        String username,
        String displayName,
        String phone,
        String email,
        String userType,
        String status
) {
}

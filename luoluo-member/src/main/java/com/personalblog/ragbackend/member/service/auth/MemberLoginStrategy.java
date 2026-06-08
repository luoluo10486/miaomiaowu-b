package com.personalblog.ragbackend.member.service.auth;

import com.personalblog.ragbackend.member.domain.MemberUser;
import com.personalblog.ragbackend.member.dto.auth.MemberLoginRequest;

/**
 * 会员登录策略接口
 */
public interface MemberLoginStrategy {
    String grantType();

    MemberUser authenticate(MemberLoginRequest request);
}

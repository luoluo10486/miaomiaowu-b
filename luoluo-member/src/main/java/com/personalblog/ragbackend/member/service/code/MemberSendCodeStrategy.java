package com.personalblog.ragbackend.member.service.code;

import com.personalblog.ragbackend.member.dto.code.MemberSendVerifyCodeRequest;
import com.personalblog.ragbackend.member.dto.code.MemberSendVerifyCodeResponse;

/**
 * 会员Send验证码策略接口
 */
public interface MemberSendCodeStrategy {
    String grantType();

    MemberSendVerifyCodeResponse send(MemberSendVerifyCodeRequest request);
}

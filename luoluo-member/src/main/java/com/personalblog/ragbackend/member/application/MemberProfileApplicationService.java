package com.personalblog.ragbackend.member.application;

import com.personalblog.ragbackend.member.dto.profile.MemberProfileResponse;
import com.personalblog.ragbackend.member.service.MemberProfileService;
import org.springframework.stereotype.Service;

/**
 * 会员资料应用服务
 */
@Service
public class MemberProfileApplicationService {
    private final MemberProfileService memberProfileService;

    public MemberProfileApplicationService(MemberProfileService memberProfileService) {
        this.memberProfileService = memberProfileService;
    }

    public MemberProfileResponse getCurrentProfile() {
        return memberProfileService.getCurrentProfile();
    }
}

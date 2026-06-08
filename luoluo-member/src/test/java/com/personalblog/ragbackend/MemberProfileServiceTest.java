package com.personalblog.ragbackend;

import com.personalblog.ragbackend.member.domain.MemberUser;
import com.personalblog.ragbackend.member.dto.profile.MemberProfileResponse;
import com.personalblog.ragbackend.member.service.MemberProfileService;
import com.personalblog.ragbackend.member.service.MemberSessionService;
import com.personalblog.ragbackend.member.service.MemberUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 会员资料服务Test类
 */
@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {
    @Mock
    private MemberSessionService memberSessionService;

    @Mock
    private MemberUserService memberUserService;

    private MemberProfileService memberProfileService;

    @BeforeEach
    void setUp() {
        memberProfileService = new MemberProfileService(memberSessionService, memberUserService);
    }

    @Test
    void getCurrentProfileShouldNormalizeUserTypeExpression() {
        MemberUser user = new MemberUser();
        user.setUserId(7L);
        user.setUsername("demo");
        user.setDisplayName("Demo");
        user.setEmail("demo@example.com");
        user.setUserType("yys");
        user.setStatus("ACTIVE");

        when(memberSessionService.getCurrentLoginUserId()).thenReturn(7L);
        when(memberUserService.findActiveById(7L)).thenReturn(user);

        MemberProfileResponse response = memberProfileService.getCurrentProfile();

        assertThat(response.userType()).isEqualTo("user,yys");
    }
}

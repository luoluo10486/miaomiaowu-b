package com.personalblog.ragbackend.rag.service.quota;

import com.personalblog.ragbackend.common.context.LoginUser;
import com.personalblog.ragbackend.common.context.UserContext;
import com.personalblog.ragbackend.member.domain.MemberUser;
import com.personalblog.ragbackend.rag.dao.entity.RagQuestionQuotaAuditEntity;
import com.personalblog.ragbackend.rag.dao.entity.RagQuestionQuotaConfigEntity;
import com.personalblog.ragbackend.rag.dao.mapper.RagQuestionQuotaAuditMapper;
import com.personalblog.ragbackend.rag.dao.mapper.RagQuestionQuotaConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyQuestionQuotaServiceTest {
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RagQuestionQuotaConfigMapper quotaConfigMapper;

    @Mock
    private RagQuestionQuotaAuditMapper quotaAuditMapper;

    private DailyQuestionQuotaService service;

    @BeforeEach
    void setUp() {
        service = new DailyQuestionQuotaService(stringRedisTemplate, quotaConfigMapper, quotaAuditMapper);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldReturnCachedLimitWithoutHittingDb() {
        when(valueOperations.get("rag:chat:daily-question-limit")).thenReturn("7");

        int limit = service.getDailyQuestionLimit();

        assertEquals(7, limit);
        verify(quotaConfigMapper, never()).selectOne(any());
    }

    @Test
    void shouldLoadLimitFromDbAndCacheItOnMiss() {
        when(valueOperations.get("rag:chat:daily-question-limit")).thenReturn(null);
        RagQuestionQuotaConfigEntity config = new RagQuestionQuotaConfigEntity();
        config.setId(1L);
        config.setConfigKey("global_daily_question_limit");
        config.setDailyLimit(12);
        config.setDeleted(0);
        when(quotaConfigMapper.selectOne(any())).thenReturn(config);

        int limit = service.getDailyQuestionLimit();

        assertEquals(12, limit);
        verify(valueOperations).set("rag:chat:daily-question-limit", "12");
    }

    @Test
    void shouldUpdateLimitAndWriteAudit() {
        setCurrentUser(42L, "admin", "superadmin");
        RagQuestionQuotaConfigEntity config = new RagQuestionQuotaConfigEntity();
        config.setId(1L);
        config.setConfigKey("global_daily_question_limit");
        config.setDailyLimit(5);
        config.setDeleted(0);
        when(quotaConfigMapper.selectOne(any())).thenReturn(config);
        when(quotaConfigMapper.updateById(org.mockito.ArgumentMatchers.<RagQuestionQuotaConfigEntity>any())).thenReturn(1);
        when(quotaAuditMapper.insert(org.mockito.ArgumentMatchers.<RagQuestionQuotaAuditEntity>any())).thenReturn(1);

        int limit = service.updateDailyQuestionLimit(8);

        assertEquals(8, limit);
        verify(quotaConfigMapper).updateById(org.mockito.ArgumentMatchers.<RagQuestionQuotaConfigEntity>any());
        ArgumentCaptor<RagQuestionQuotaAuditEntity> captor = ArgumentCaptor.forClass(RagQuestionQuotaAuditEntity.class);
        verify(quotaAuditMapper).insert(captor.capture());
        RagQuestionQuotaAuditEntity audit = captor.getValue();
        assertEquals("UPDATE_LIMIT", audit.getActionType());
        assertEquals(42L, audit.getActorUserId());
        assertEquals("admin", audit.getActorUsername());
        assertEquals("superadmin", audit.getActorRole());
        assertEquals(5, audit.getOldDailyLimit());
        assertEquals(8, audit.getNewDailyLimit());
        verify(valueOperations).set("rag:chat:daily-question-limit", "8");
    }

    @Test
    void shouldRejectWhenQuotaExceeded() {
        setCurrentUser(7L, "reader", "user");
        when(valueOperations.get("rag:chat:daily-question-limit")).thenReturn("5");
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(List.of(0L, 5L));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.assertCanAskCurrentUser());

        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    void shouldBypassForSuperAdmin() {
        setCurrentUser(1L, "admin", "superadmin");

        service.assertCanAskCurrentUser();

        verify(stringRedisTemplate, never()).execute(any(), anyList(), anyString(), anyString());
    }

    @Test
    void shouldResetUserCountAndWriteAudit() {
        setCurrentUser(42L, "admin", "superadmin");
        MemberUser user = new MemberUser();
        user.setUserId(99L);
        user.setUsername("alice");

        when(quotaAuditMapper.insert(org.mockito.ArgumentMatchers.<RagQuestionQuotaAuditEntity>any())).thenReturn(1);

        service.resetTodayCountForUser(user);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(stringRedisTemplate).delete(keyCaptor.capture());
        String key = keyCaptor.getValue();
        assertTrue(key.startsWith("rag:chat:daily-question-count:99:"));
        assertEquals(LocalDate.now().toString(), key.substring(key.lastIndexOf(':') + 1));

        ArgumentCaptor<RagQuestionQuotaAuditEntity> auditCaptor = ArgumentCaptor.forClass(RagQuestionQuotaAuditEntity.class);
        verify(quotaAuditMapper).insert(auditCaptor.capture());
        RagQuestionQuotaAuditEntity audit = auditCaptor.getValue();
        assertEquals("RESET_USER_COUNT", audit.getActionType());
        assertEquals(42L, audit.getActorUserId());
        assertEquals("alice", audit.getTargetUsername());
        assertEquals(99L, audit.getTargetUserId());
    }

    private void setCurrentUser(Long userId, String username, String role) {
        LoginUser user = new LoginUser();
        user.setUserId(userId == null ? null : String.valueOf(userId));
        user.setUsername(username);
        user.setDisplayName(username);
        user.setRole(role);
        UserContext.set(user);
    }
}

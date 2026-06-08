package com.personalblog.ragbackend.rag.service.quota;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personalblog.ragbackend.common.auth.RoleUtils;
import com.personalblog.ragbackend.common.context.UserContext;
import com.personalblog.ragbackend.member.domain.MemberUser;
import com.personalblog.ragbackend.rag.dao.entity.RagQuestionQuotaAuditEntity;
import com.personalblog.ragbackend.rag.dao.entity.RagQuestionQuotaConfigEntity;
import com.personalblog.ragbackend.rag.dao.mapper.RagQuestionQuotaAuditMapper;
import com.personalblog.ragbackend.rag.dao.mapper.RagQuestionQuotaConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyQuestionQuotaService {
    public static final int DEFAULT_DAILY_LIMIT = 5;
    private static final String DAILY_LIMIT_KEY = "rag:chat:daily-question-limit";
    private static final String DAILY_COUNT_KEY_PREFIX = "rag:chat:daily-question-count:";
    private static final String GLOBAL_CONFIG_KEY = "global_daily_question_limit";
    private static final String ACTION_UPDATE_LIMIT = "UPDATE_LIMIT";
    private static final String ACTION_RESET_USER = "RESET_USER_COUNT";
    private static final DefaultRedisScript<List> CONSUME_SCRIPT = new DefaultRedisScript<>();

    static {
        CONSUME_SCRIPT.setScriptText("""
                local current = redis.call('GET', KEYS[1])
                local limit = tonumber(ARGV[1])
                local ttl = tonumber(ARGV[2])
                if not current then
                    redis.call('SET', KEYS[1], 1)
                    if ttl > 0 then
                        redis.call('EXPIRE', KEYS[1], ttl)
                    end
                    return {1, 1}
                end
                local nextValue = tonumber(current) + 1
                if nextValue > limit then
                    return {0, tonumber(current)}
                end
                redis.call('INCR', KEYS[1])
                return {1, nextValue}
                """);
        CONSUME_SCRIPT.setResultType(List.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final RagQuestionQuotaConfigMapper quotaConfigMapper;
    private final RagQuestionQuotaAuditMapper quotaAuditMapper;

    public int getDailyQuestionLimit() {
        Integer cachedLimit = readCachedLimit();
        if (cachedLimit != null) {
            return cachedLimit;
        }

        RagQuestionQuotaConfigEntity config = loadOrCreateConfig();
        int limit = normalizeLimit(config == null ? null : config.getDailyLimit());
        cacheDailyLimit(limit);
        return limit;
    }

    public int updateDailyQuestionLimit(int limit) {
        int normalized = normalizeLimit(limit);
        RagQuestionQuotaConfigEntity config = loadOrCreateConfig();
        Integer oldLimit = config == null ? null : config.getDailyLimit();
        LocalDateTime now = LocalDateTime.now();

        if (config == null) {
            config = new RagQuestionQuotaConfigEntity();
            config.setConfigKey(GLOBAL_CONFIG_KEY);
            config.setDailyLimit(normalized);
            config.setRemark("全局每日提问上限");
            config.setDeleted(0);
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            quotaConfigMapper.insert(config);
        } else {
            config.setDailyLimit(normalized);
            config.setRemark("全局每日提问上限");
            config.setUpdatedAt(now);
            quotaConfigMapper.updateById(config);
        }

        try {
            insertAudit(
                    ACTION_UPDATE_LIMIT,
                    null,
                    null,
                    oldLimit,
                    normalized,
                    "管理员修改每日提问上限"
            );
        } catch (Exception ex) {
            log.warn("Failed to write daily question quota update audit", ex);
        }
        cacheDailyLimit(normalized);
        return normalized;
    }

    public void resetTodayCountForUser(MemberUser user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        String userId = String.valueOf(user.getUserId());
        stringRedisTemplate.delete(buildDailyCountKey(userId));
        try {
            insertAudit(
                    ACTION_RESET_USER,
                    user.getUserId(),
                    user.getUsername(),
                    null,
                    null,
                    "重置用户今日提问次数"
            );
        } catch (Exception ex) {
            log.warn("Failed to write daily question quota reset audit", ex);
        }
    }

    public void assertCanAskCurrentUser() {
        if (RoleUtils.isSuperAdmin(UserContext.getRole())) {
            return;
        }

        String userId = normalizeUserId(UserContext.getUserId());
        if (userId == null) {
            throw new IllegalArgumentException("未获取到当前登录用户");
        }

        int limit = getDailyQuestionLimit();
        long ttlSeconds = secondsUntilTomorrow();
        List<?> result = stringRedisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(buildDailyCountKey(userId)),
                String.valueOf(limit),
                String.valueOf(ttlSeconds)
        );
        if (result == null || result.size() < 2) {
            throw new IllegalStateException("每日提问配额校验失败");
        }

        long allowedFlag = parseLong(result.get(0));
        long usedToday = parseLong(result.get(1));
        if (allowedFlag != 1L) {
            throw new IllegalArgumentException(buildLimitMessage((int) usedToday, limit));
        }
    }

    private RagQuestionQuotaConfigEntity loadOrCreateConfig() {
        RagQuestionQuotaConfigEntity config = quotaConfigMapper.selectOne(
                Wrappers.<RagQuestionQuotaConfigEntity>lambdaQuery()
                        .eq(RagQuestionQuotaConfigEntity::getConfigKey, GLOBAL_CONFIG_KEY)
                        .eq(RagQuestionQuotaConfigEntity::getDeleted, 0)
                        .last("limit 1")
        );
        if (config != null) {
            return config;
        }

        LocalDateTime now = LocalDateTime.now();
        RagQuestionQuotaConfigEntity defaultConfig = new RagQuestionQuotaConfigEntity();
        defaultConfig.setConfigKey(GLOBAL_CONFIG_KEY);
        defaultConfig.setDailyLimit(DEFAULT_DAILY_LIMIT);
        defaultConfig.setRemark("全局每日提问上限");
        defaultConfig.setDeleted(0);
        defaultConfig.setCreatedAt(now);
        defaultConfig.setUpdatedAt(now);
        try {
            quotaConfigMapper.insert(defaultConfig);
            return defaultConfig;
        } catch (DuplicateKeyException ignored) {
            return quotaConfigMapper.selectOne(
                    Wrappers.<RagQuestionQuotaConfigEntity>lambdaQuery()
                            .eq(RagQuestionQuotaConfigEntity::getConfigKey, GLOBAL_CONFIG_KEY)
                            .eq(RagQuestionQuotaConfigEntity::getDeleted, 0)
                            .last("limit 1")
            );
        }
    }

    private void insertAudit(String actionType,
                             Long targetUserId,
                             String targetUsername,
                             Integer oldLimit,
                             Integer newLimit,
                             String remark) {
        Long actorUserId = resolveCurrentUserId();
        if (actorUserId == null) {
            throw new IllegalArgumentException("未获取到当前登录用户");
        }

        LocalDateTime now = LocalDateTime.now();
        RagQuestionQuotaAuditEntity audit = new RagQuestionQuotaAuditEntity();
        audit.setActionType(actionType);
        audit.setActorUserId(actorUserId);
        audit.setActorUsername(resolveCurrentUsername());
        audit.setActorRole(resolveCurrentRole());
        audit.setTargetUserId(targetUserId);
        audit.setTargetUsername(targetUsername);
        audit.setOldDailyLimit(oldLimit);
        audit.setNewDailyLimit(newLimit);
        audit.setRemark(remark);
        audit.setCreatedAt(now);
        audit.setUpdatedAt(now);
        quotaAuditMapper.insert(audit);
    }

    private void cacheDailyLimit(int limit) {
        try {
            stringRedisTemplate.opsForValue().set(DAILY_LIMIT_KEY, String.valueOf(limit));
        } catch (Exception ex) {
            log.warn("Failed to cache daily question limit in Redis", ex);
        }
    }

    private Integer readCachedLimit() {
        try {
            String raw = stringRedisTemplate.opsForValue().get(DAILY_LIMIT_KEY);
            if (StrUtil.isBlank(raw)) {
                return null;
            }
            return normalizeLimit(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ex) {
            return null;
        } catch (Exception ex) {
            log.warn("Failed to read cached daily question limit from Redis", ex);
            return null;
        }
    }

    private int normalizeLimit(Integer limit) {
        return Math.max(1, limit == null ? DEFAULT_DAILY_LIMIT : limit);
    }

    private String buildLimitMessage(int usedToday, int limit) {
        return "您今天的提问次数已达上限（" + usedToday + "/" + limit + "），请明天再试，或联系管理员重置。";
    }

    private long secondsUntilTomorrow() {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime tomorrowStart = now.toLocalDate().plusDays(1).atStartOfDay(zoneId);
        return Math.max(1L, Duration.between(now, tomorrowStart).getSeconds());
    }

    private long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private String normalizeUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return null;
        }
        return userId.trim();
    }

    private String buildDailyCountKey(String userId) {
        return DAILY_COUNT_KEY_PREFIX + userId + ":" + ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate();
    }

    private Long resolveCurrentUserId() {
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            return null;
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolveCurrentUsername() {
        String username = UserContext.getUsername();
        if (StrUtil.isNotBlank(username)) {
            return username.trim();
        }
        String displayName = UserContext.getDisplayName();
        return StrUtil.blankToDefault(displayName, null);
    }

    private String resolveCurrentRole() {
        String role = UserContext.getRole();
        return StrUtil.blankToDefault(role, null);
    }
}

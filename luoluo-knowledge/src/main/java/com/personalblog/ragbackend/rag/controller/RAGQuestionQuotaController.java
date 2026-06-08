package com.personalblog.ragbackend.rag.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.personalblog.ragbackend.common.auth.RoleUtils;
import com.personalblog.ragbackend.common.satoken.annotation.MemberLoginRequired;
import com.personalblog.ragbackend.common.web.domain.Result;
import com.personalblog.ragbackend.common.web.domain.Results;
import com.personalblog.ragbackend.member.domain.MemberUser;
import com.personalblog.ragbackend.member.service.MemberUserService;
import com.personalblog.ragbackend.rag.controller.request.DailyQuestionLimitUpdateRequest;
import com.personalblog.ragbackend.rag.service.quota.DailyQuestionQuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG问题Quota控制器
 */
@RestController
@RequiredArgsConstructor
@MemberLoginRequired
public class RAGQuestionQuotaController {
    private final DailyQuestionQuotaService dailyQuestionQuotaService;
    private final MemberUserService memberUserService;

    @PutMapping("/rag/question-quota/daily-limit")
    public Result<Integer> updateDailyLimit(@Valid @RequestBody DailyQuestionLimitUpdateRequest request) {
        StpUtil.checkRole(RoleUtils.ROLE_SUPER_ADMIN);
        int normalized = dailyQuestionQuotaService.updateDailyQuestionLimit(request.getLimit());
        return Results.success(normalized);
    }

    @PostMapping("/rag/question-quota/users/{id}/reset")
    public Result<Void> resetUserDailyCount(@PathVariable String id) {
        StpUtil.checkRole(RoleUtils.ROLE_SUPER_ADMIN);
        MemberUser user = memberUserService.findActiveById(parseUserId(id));
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        dailyQuestionQuotaService.resetTodayCountForUser(user);
        return Results.success();
    }

    private Long parseUserId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("用户 ID 格式不正确");
        }
    }
}

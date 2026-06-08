package com.personalblog.ragbackend.member.controller;

import com.personalblog.ragbackend.common.satoken.annotation.MemberLoginRequired;
import com.personalblog.ragbackend.common.web.domain.Result;
import com.personalblog.ragbackend.common.web.domain.Results;
import com.personalblog.ragbackend.member.application.MemberAuthApplicationService;
import com.personalblog.ragbackend.member.dto.auth.MemberLoginRequest;
import com.personalblog.ragbackend.member.dto.auth.MemberLoginResponse;
import com.personalblog.ragbackend.member.dto.auth.MemberRegisterRequest;
import com.personalblog.ragbackend.member.dto.code.MemberSendVerifyCodeRequest;
import com.personalblog.ragbackend.member.dto.code.MemberSendVerifyCodeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员认证控制器
 */
@RestController
@RequestMapping("/luoluo/member/auth")
public class MemberAuthController {
    private final MemberAuthApplicationService memberAuthApplicationService;

    public MemberAuthController(MemberAuthApplicationService memberAuthApplicationService) {
        this.memberAuthApplicationService = memberAuthApplicationService;
    }

    @PostMapping("/login")
    public Result<MemberLoginResponse> login(@Valid @RequestBody MemberLoginRequest requestParam, HttpServletRequest servletRequest) {
        return Results.success(memberAuthApplicationService.login(requestParam, resolveClientIp(servletRequest)))
                .setMessage("login success");
    }

    @PostMapping("/register")
    public Result<MemberLoginResponse> register(@Valid @RequestBody MemberRegisterRequest requestParam, HttpServletRequest servletRequest) {
        return Results.success(memberAuthApplicationService.register(requestParam, resolveClientIp(servletRequest)))
                .setMessage("register success");
    }

    @PostMapping("/logout")
    @MemberLoginRequired
    public Result<Void> logout() {
        memberAuthApplicationService.logout();
        return Results.success().setMessage("logout success");
    }

    @PostMapping("/send-code")
    public Result<MemberSendVerifyCodeResponse> sendCode(@Valid @RequestBody MemberSendVerifyCodeRequest requestParam) {
        return Results.success(memberAuthApplicationService.sendCode(requestParam))
                .setMessage("send code success");
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return null;
        }
        return remoteAddr.trim();
    }
}

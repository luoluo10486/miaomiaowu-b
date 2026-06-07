package com.personalblog.ragbackend.member.service;

import com.personalblog.ragbackend.member.domain.MemberUser;
import com.personalblog.ragbackend.member.dto.auth.MemberLoginResponse;
import com.personalblog.ragbackend.member.dto.auth.MemberRegisterRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class MemberRegisterService {
    private static final String USER_TYPE = "user";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final MemberUserService memberUserService;
    private final MemberVerifyCodeService memberVerifyCodeService;
    private final PasswordEncoder passwordEncoder;
    private final MemberAuthService memberAuthService;

    public MemberRegisterService(
            MemberUserService memberUserService,
            MemberVerifyCodeService memberVerifyCodeService,
            PasswordEncoder passwordEncoder,
            MemberAuthService memberAuthService
    ) {
        this.memberUserService = memberUserService;
        this.memberVerifyCodeService = memberVerifyCodeService;
        this.passwordEncoder = passwordEncoder;
        this.memberAuthService = memberAuthService;
    }

    @Transactional
    public MemberLoginResponse register(MemberRegisterRequest request, String clientIp) {
        String grantType = normalizeGrantType(request.getGrantType());
        String rawPassword = requirePassword(request.getPassword());
        validateConfirmPassword(rawPassword, request.getConfirmPassword());

        String phone = normalizePhone(request.getPhone());
        String email = normalizeEmail(request.getEmail());
        String username = normalizeUsername(request.getUsername());

        switch (grantType) {
            case "password" -> {
                if (username == null) {
                    throw new ResponseStatusException(BAD_REQUEST, "用户名不能为空");
                }
            }
            case "sms" -> {
                if (phone == null) {
                    throw new ResponseStatusException(BAD_REQUEST, "手机号不能为空");
                }
                if (username == null) {
                    username = phone;
                }
            }
            case "email" -> {
                if (email == null) {
                    throw new ResponseStatusException(BAD_REQUEST, "邮箱不能为空");
                }
                if (username == null) {
                    username = email;
                }
            }
            default -> throw new ResponseStatusException(BAD_REQUEST, "不支持的授权类型：" + grantType);
        }

        ensureUnique(username, phone, email);

        if ("sms".equals(grantType)) {
            verifySmsCode(phone, request.getSmsCode());
        }
        if ("email".equals(grantType)) {
            verifyEmailCode(email, request.getEmailCode());
        }

        MemberUser user = new MemberUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setEmail(email);
        user.setDisplayName(normalizeDisplayName(request.getDisplayName(), username));
        user.setUserType(USER_TYPE);
        user.setStatus(ACTIVE_STATUS);
        user.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        MemberUser createdUser;
        try {
            createdUser = memberUserService.create(user);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(CONFLICT, "账号信息已存在", exception);
        }

        return memberAuthService.createLoginResponse(createdUser, grantType, request.getDeviceType(), clientIp);
    }

    private void verifySmsCode(String phone, String smsCode) {
        if (smsCode == null || smsCode.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "短信验证码不能为空");
        }
        boolean verified = memberVerifyCodeService.verifyRegisterOrLoginCodeAndConsume("sms", phone, smsCode.trim());
        if (!verified) {
            throw new ResponseStatusException(BAD_REQUEST, "短信验证码无效或已过期");
        }
    }

    private void verifyEmailCode(String email, String emailCode) {
        if (emailCode == null || emailCode.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "邮箱验证码不能为空");
        }
        boolean verified = memberVerifyCodeService.verifyRegisterOrLoginCodeAndConsume("email", email, emailCode.trim());
        if (!verified) {
            throw new ResponseStatusException(BAD_REQUEST, "邮箱验证码无效或已过期");
        }
    }

    private void ensureUnique(String username, String phone, String email) {
        if (memberUserService.existsByUsername(username)) {
            throw new ResponseStatusException(CONFLICT, "用户名已存在");
        }
        if (phone != null && memberUserService.existsByPhone(phone)) {
            throw new ResponseStatusException(CONFLICT, "手机号已存在");
        }
        if (email != null && memberUserService.existsByEmail(email)) {
            throw new ResponseStatusException(CONFLICT, "邮箱已存在");
        }
    }

    private String normalizeGrantType(String grantType) {
        if (grantType == null || grantType.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "授权类型不能为空");
        }
        return grantType.trim().toLowerCase(Locale.ROOT);
    }

    private String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "密码不能为空");
        }
        return password;
    }

    private void validateConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword == null || confirmPassword.isBlank()) {
            return;
        }
        if (!password.equals(confirmPassword)) {
            throw new ResponseStatusException(BAD_REQUEST, "确认密码与密码不一致");
        }
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return username.trim();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String displayName, String fallbackUsername) {
        if (displayName == null || displayName.isBlank()) {
            return fallbackUsername;
        }
        return displayName.trim();
    }
}

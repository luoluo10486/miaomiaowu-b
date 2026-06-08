package com.personalblog.ragbackend.member.config;

import com.personalblog.ragbackend.member.domain.MemberUser;
import com.personalblog.ragbackend.member.service.MemberUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 会员管理端BootstrapRunner类
 */
@Component
@EnableConfigurationProperties(MemberAdminBootstrapProperties.class)
public class MemberAdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MemberAdminBootstrapRunner.class);
    private static final String ROLE_ADMIN = "superadmin";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final MemberUserService memberUserService;
    private final PasswordEncoder passwordEncoder;
    private final MemberAdminBootstrapProperties properties;

    public MemberAdminBootstrapRunner(MemberUserService memberUserService,
                                      PasswordEncoder passwordEncoder,
                                      MemberAdminBootstrapProperties properties) {
        this.memberUserService = memberUserService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        MemberUser existingAdmin = memberUserService.findAnyAdmin();
        if (existingAdmin != null) {
            return;
        }

        String username = normalizeOrDefault(properties.getUsername(), "admin");
        String password = normalizeOrDefault(properties.getPassword(), "admin123456");
        String displayName = normalizeOrDefault(properties.getDisplayName(), "Admin");
        String email = normalizeOrDefault(properties.getEmail(), "admin@example.com");

        MemberUser admin = new MemberUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(displayName);
        admin.setEmail(email);
        admin.setUserType(ROLE_ADMIN);
        admin.setStatus(STATUS_ACTIVE);
        admin.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        memberUserService.create(admin);

        log.info("No admin user detected. Bootstrapped default admin account: username={}", username);
    }

    private String normalizeOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}

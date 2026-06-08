package com.personalblog.ragbackend.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会员管理端Bootstrap配置属性
 */
@ConfigurationProperties(prefix = "app.member.bootstrap.admin")
public class MemberAdminBootstrapProperties {
    /**
     * Whether to auto-create a default admin account when none exists.
     */
    private boolean enabled = true;

    private String username = "admin";

    private String password = "admin123456";

    private String displayName = "Admin";

    private String email = "admin@example.com";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

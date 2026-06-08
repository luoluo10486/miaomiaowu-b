package com.personalblog.ragbackend.common.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 角色工具类
 */
public final class RoleUtils {
    public static final String ROLE_USER = "user";
    public static final String ROLE_SUPER_ADMIN = "superadmin";

    private RoleUtils() {
    }

    public static Set<String> parseRoles(String roleExpression) {
        Set<String> roles = new LinkedHashSet<>();
        if (roleExpression == null || roleExpression.isBlank()) {
            return roles;
        }

        String[] tokens = roleExpression.split(",");
        for (String token : tokens) {
            String normalized = normalizeRoleToken(token);
            if (normalized != null) {
                roles.add(normalized);
            }
        }
        return roles;
    }

    public static String normalizeRoleExpression(String roleExpression) {
        Set<String> roles = parseRoles(roleExpression);
        if (roles.isEmpty()) {
            return null;
        }
        return String.join(",", roles);
    }

    public static String normalizeUserTypeExpression(String roleExpression) {
        Set<String> roles = parseRoles(roleExpression);
        if (roles.isEmpty()) {
            return ROLE_USER;
        }
        if (roles.contains(ROLE_SUPER_ADMIN)) {
            return ROLE_SUPER_ADMIN;
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        normalized.add(ROLE_USER);
        normalized.addAll(roles);
        return String.join(",", normalized);
    }

    public static boolean hasAnyRole(String roleExpression, Collection<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return false;
        }
        Set<String> roles = parseRoles(roleExpression);
        if (roles.isEmpty()) {
            return false;
        }
        for (String requiredRole : requiredRoles) {
            String normalized = normalizeRoleToken(requiredRole);
            if (normalized != null && roles.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSuperAdmin(String roleExpression) {
        return parseRoles(roleExpression).contains(ROLE_SUPER_ADMIN);
    }

    public static String normalizeRoleToken(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "admin", ROLE_SUPER_ADMIN -> ROLE_SUPER_ADMIN;
            case "user" -> ROLE_USER;
            default -> normalized;
        };
    }

    public static List<String> normalizeRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String role : roles) {
            String value = normalizeRoleToken(role);
            if (value != null) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }
}

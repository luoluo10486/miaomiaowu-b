package com.personalblog.ragbackend.rag.util;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

public final class MarkdownContentSanitizer {
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*]\\([^\\r\\n]*\\)");
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile("(?is)<img\\b[^>]*>");

    private MarkdownContentSanitizer() {
    }

    public static String stripImages(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }

        String sanitized = content
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        sanitized = MARKDOWN_IMAGE_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = HTML_IMAGE_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.replaceAll("[ \\t]{2,}", " ");
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n");
        return sanitized.trim();
    }
}

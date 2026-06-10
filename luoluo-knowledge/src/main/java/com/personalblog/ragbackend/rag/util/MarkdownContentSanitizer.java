package com.personalblog.ragbackend.rag.util;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * Markdown内容Sanitizer类
 */
public final class MarkdownContentSanitizer {
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*]\\([^\\r\\n]*\\)");
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile("(?is)<img\\b[^>]*>");
    private static final Pattern TOOL_CALL_BLOCK_PATTERN = Pattern.compile("(?is)<tool-call\\b[^>]*>.*?</tool-call\\s*>");
    private static final Pattern TOOL_CALL_TAG_PATTERN = Pattern.compile("(?is)</?tool-call\\b[^>]*>");
    private static final Pattern FUNCTION_TAG_PATTERN = Pattern.compile("(?is)</?function(?:=[^>]+)?>");
    private static final Pattern PARAMETER_TAG_PATTERN = Pattern.compile("(?is)</?parameter(?:=[^>]+)?>");

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

    public static String stripToolCallArtifacts(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }

        String sanitized = content
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        sanitized = TOOL_CALL_BLOCK_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = TOOL_CALL_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = FUNCTION_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = PARAMETER_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.replaceAll("[ \\t]{2,}", " ");
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n");
        return sanitized.trim();
    }

    public static String stripImagesAndToolCallArtifacts(String content) {
        return stripToolCallArtifacts(stripImages(content));
    }
}

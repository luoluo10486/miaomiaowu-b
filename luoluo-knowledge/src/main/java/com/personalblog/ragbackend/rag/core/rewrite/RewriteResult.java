package com.personalblog.ragbackend.rag.core.rewrite;

import java.util.List;

/**
 * 改写结果记录类
 */
public record RewriteResult(String rewrittenQuestion, List<String> subQuestions) {
}

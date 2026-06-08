package com.personalblog.ragbackend.rag.core.intent;

/**
 * 引导Decision记录类
 */
public record GuidanceDecision(
        boolean prompt,
        String promptText
) {
    public static GuidanceDecision none() {
        return new GuidanceDecision(false, null);
    }

    public static GuidanceDecision prompt(String promptText) {
        return new GuidanceDecision(true, promptText);
    }
}

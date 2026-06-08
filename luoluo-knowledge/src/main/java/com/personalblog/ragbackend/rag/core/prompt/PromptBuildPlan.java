package com.personalblog.ragbackend.rag.core.prompt;

import lombok.Builder;
import lombok.Data;

/**
 * 提示词BuildPlan类
 */
@Data
@Builder
public class PromptBuildPlan {
    private PromptScene scene;
    private String baseTemplate;
    private String mcpContext;
    private String kbContext;
    private String question;
}

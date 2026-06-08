package com.personalblog.ragbackend.rag.core.prompt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.personalblog.ragbackend.infra.convention.ChatMessage;
import com.personalblog.ragbackend.infra.convention.RetrievedChunk;
import com.personalblog.ragbackend.rag.core.prompt.PromptTemplateLoader;
import com.personalblog.ragbackend.rag.constant.RAGConstant;
import com.personalblog.ragbackend.rag.core.intent.IntentNode;
import com.personalblog.ragbackend.rag.core.intent.NodeScore;
import com.personalblog.ragbackend.rag.util.MarkdownContentSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RAG提示词服务
 */
@Service
@Slf4j
public class RAGPromptService {
    private static final int MAX_CITATION_EXCERPT_LENGTH = 220;
    private static final int DEFAULT_MAX_CITATION_COUNT = 3;
    private static final int BOOSTED_MAX_CITATION_COUNT = 5;
    private static final double BOOSTED_CITATION_SCORE_THRESHOLD = 0.9D;

    private final PromptTemplateLoader promptTemplateLoader;

    public RAGPromptService(PromptTemplateLoader promptTemplateLoader) {
        this.promptTemplateLoader = promptTemplateLoader;
    }

    public String buildSystemPrompt(PromptContext context) {
        PromptBuildPlan plan = plan(context);
        String template = StrUtil.isNotBlank(plan.getBaseTemplate())
                ? plan.getBaseTemplate()
                : defaultTemplate(plan.getScene());
        String contract = (plan.getScene() == PromptScene.MCP_ONLY || plan.getScene() == PromptScene.EMPTY)
                ? ""
                : promptTemplateLoader.load(RAGConstant.RAG_ANSWER_CONTRACT_PROMPT_PATH);
        String mergedPrompt = mergePrompt(template, contract);
        return StrUtil.isBlank(mergedPrompt) ? "" : PromptTemplateUtils.cleanupPrompt(mergedPrompt);
    }

    public List<ChatMessage> buildStructuredMessages(PromptContext context,
                                                     List<ChatMessage> history,
                                                     String question,
                                                     List<String> subQuestions) {
        List<ChatMessage> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(context);
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        if (CollUtil.isNotEmpty(history)) {
            messages.addAll(history);
        }

        String evidenceBody = buildEvidenceBody(context);
        String userQuestion = buildUserQuestion(question, subQuestions);
        String mergedUserContent = mergeEvidenceAndQuestion(evidenceBody, userQuestion);
        if (StrUtil.isNotBlank(mergedUserContent)) {
            messages.add(ChatMessage.user(mergedUserContent));
        }
        return messages;
    }

    private PromptBuildPlan plan(PromptContext context) {
        if (context == null) {
            throw new IllegalStateException("PromptContext requires MCP or KB context.");
        }
        if (context.hasMcp() && !context.hasKb()) {
            return planMcpOnly(context);
        }
        if (!context.hasMcp() && context.hasKb()) {
            return planKbOnly(context);
        }
        if (context.hasMcp() && context.hasKb()) {
            return planMixed(context);
        }
        throw new IllegalStateException("PromptContext requires MCP or KB context.");
    }

    private PromptBuildPlan planKbOnly(PromptContext context) {
        PromptPlan plan = planPrompt(context.getKbIntents(), context.getIntentChunks());
        return PromptBuildPlan.builder()
                .scene(PromptScene.KB_ONLY)
                .baseTemplate(plan.getBaseTemplate())
                .mcpContext(context.getMcpContext())
                .kbContext(context.getKbContext())
                .question(context.getQuestion())
                .build();
    }

    private PromptBuildPlan planMcpOnly(PromptContext context) {
        List<NodeScore> intents = context.getMcpIntents();
        String baseTemplate = null;
        if (CollUtil.isNotEmpty(intents) && intents.size() == 1) {
            IntentNode node = intents.get(0).node();
            if (node != null && StrUtil.isNotBlank(node.getPromptTemplate())) {
                baseTemplate = node.getPromptTemplate();
            }
        }
        return PromptBuildPlan.builder()
                .scene(PromptScene.MCP_ONLY)
                .baseTemplate(baseTemplate)
                .mcpContext(context.getMcpContext())
                .kbContext(context.getKbContext())
                .question(context.getQuestion())
                .build();
    }

    private PromptBuildPlan planMixed(PromptContext context) {
        return PromptBuildPlan.builder()
                .scene(PromptScene.MIXED)
                .mcpContext(context.getMcpContext())
                .kbContext(context.getKbContext())
                .question(context.getQuestion())
                .build();
    }

    private PromptPlan planPrompt(List<NodeScore> intents, Map<String, List<RetrievedChunk>> intentChunks) {
        List<NodeScore> safeIntents = intents == null ? Collections.emptyList() : intents;
        List<NodeScore> retained = safeIntents.stream()
                .filter(ns -> {
                    IntentNode node = ns == null ? null : ns.node();
                    String key = nodeKey(node);
                    List<RetrievedChunk> chunks = intentChunks == null ? null : intentChunks.get(key);
                    return CollUtil.isNotEmpty(chunks);
                })
                .toList();

        if (retained.isEmpty()) {
            return new PromptPlan(Collections.emptyList(), null);
        }
        if (retained.size() == 1) {
            IntentNode only = retained.get(0).node();
            String tpl = only == null ? "" : StrUtil.emptyIfNull(only.getPromptTemplate()).trim();
            if (StrUtil.isNotBlank(tpl)) {
                return new PromptPlan(retained, tpl);
            }
        }
        return new PromptPlan(retained, null);
    }

    private String defaultTemplate(PromptScene scene) {
        return switch (scene) {
            case KB_ONLY -> promptTemplateLoader.load(RAGConstant.RAG_ENTERPRISE_PROMPT_PATH);
            case MCP_ONLY -> promptTemplateLoader.load(RAGConstant.MCP_ONLY_PROMPT_PATH);
            case MIXED -> promptTemplateLoader.load(RAGConstant.MCP_KB_MIXED_PROMPT_PATH);
            case EMPTY -> "";
        };
    }

    private String buildEvidenceBody(PromptContext context) {
        if (context == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(context.getMcpContext())) {
            sb.append(renderSection("mcp-evidence", Map.of("body", context.getMcpContext().trim())));
        }
        if (StrUtil.isNotBlank(context.getKbContext())) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(renderSection("kb-evidence", Map.of("body", context.getKbContext().trim())));
        }
        String citationBody = buildCitationBody(context.getIntentChunks());
        if (StrUtil.isNotBlank(citationBody)) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(citationBody);
        }
        return sb.toString().trim();
    }

    private String buildUserQuestion(String question, List<String> subQuestions) {
        if (CollUtil.isNotEmpty(subQuestions) && subQuestions.size() > 1) {
            String numbered = IntStream.range(0, subQuestions.size())
                    .mapToObj(i -> (i + 1) + ". " + subQuestions.get(i))
                    .collect(Collectors.joining("\n"));
            return renderSection("multi-questions", Map.of("questions", numbered));
        }
        if (StrUtil.isBlank(question)) {
            return "";
        }
        return renderSection("single-question", Map.of("question", question));
    }

    private String mergeEvidenceAndQuestion(String evidenceBody, String question) {
        if (StrUtil.isBlank(evidenceBody)) {
            return question;
        }
        if (StrUtil.isBlank(question)) {
            return evidenceBody;
        }
        return evidenceBody + "\n\n" + question;
    }

    private String buildCitationBody(Map<String, List<RetrievedChunk>> intentChunks) {
        List<RetrievedChunk> chunks = flattenChunks(intentChunks);
        if (CollUtil.isEmpty(chunks)) {
            return "";
        }

        int limit = resolveCitationLimit(chunks);
        List<String> citations = new ArrayList<>();
        for (int index = 0; index < Math.min(chunks.size(), limit); index++) {
            RetrievedChunk chunk = chunks.get(index);
            Map<String, Object> metadata = chunk == null ? null : chunk.getMetadata();
            citations.add(renderSection("kb-citation-item", Map.of(
                    "index", String.valueOf(index + 1),
                    "title", metadataText(metadata, "title", metadataText(metadata, "docName", metadataText(metadata, "documentTitle", "未命名文档"))),
                    "source_url", metadataText(metadata, "sourceUrl", metadataText(metadata, "source_url", "未提供 URL")),
                    "chunk_index", metadataText(metadata, "chunkIndex", metadataText(metadata, "chunk_index", String.valueOf(index + 1))),
                    "score", formatScore(chunk == null ? null : chunk.getScore()),
                    "excerpt", buildExcerpt(chunk == null ? null : chunk.getText())
            )));
        }

        return renderSection("kb-citations", Map.of("items", String.join("\n", citations)));
    }

    private List<RetrievedChunk> flattenChunks(Map<String, List<RetrievedChunk>> intentChunks) {
        if (intentChunks == null || intentChunks.isEmpty()) {
            return List.of();
        }

        Map<String, RetrievedChunk> uniqueChunks = new LinkedHashMap<>();
        for (List<RetrievedChunk> chunks : intentChunks.values()) {
            if (CollUtil.isEmpty(chunks)) {
                continue;
            }
            for (RetrievedChunk chunk : chunks) {
                if (chunk == null) {
                    continue;
                }
                String key = buildChunkKey(chunk);
                uniqueChunks.putIfAbsent(key, chunk);
            }
        }
        return uniqueChunks.values().stream()
                .sorted((left, right) -> Double.compare(scoreOrDefault(right), scoreOrDefault(left)))
                .toList();
    }

    private int resolveCitationLimit(List<RetrievedChunk> chunks) {
        if (CollUtil.isEmpty(chunks)) {
            return 0;
        }
        return scoreOrDefault(chunks.get(0)) >= BOOSTED_CITATION_SCORE_THRESHOLD
                ? BOOSTED_MAX_CITATION_COUNT
                : DEFAULT_MAX_CITATION_COUNT;
    }

    private String buildChunkKey(RetrievedChunk chunk) {
        Map<String, Object> metadata = chunk.getMetadata();
        String title = metadataText(metadata, "title", "");
        String sourceUrl = metadataText(metadata, "sourceUrl", "");
        String chunkIndex = metadataText(metadata, "chunkIndex", metadataText(metadata, "chunk_index", ""));
        String documentId = metadataText(metadata, "documentId", metadataText(metadata, "docId", ""));
        String text = StrUtil.blankToDefault(chunk.getText(), "");
        return String.join("|", title, sourceUrl, chunkIndex, documentId, text);
    }

    private String metadataText(Map<String, Object> metadata, String key, String defaultValue) {
        if (metadata == null || StrUtil.isBlank(key)) {
            return defaultValue;
        }
        Object value = metadata.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return StrUtil.isBlank(text) ? defaultValue : text;
    }

    private String formatScore(Float score) {
        if (score == null) {
            return "--";
        }
        return String.format(java.util.Locale.ROOT, "%.4f", score);
    }

    private double scoreOrDefault(RetrievedChunk chunk) {
        if (chunk == null || chunk.getScore() == null) {
            return 0D;
        }
        return chunk.getScore();
    }

    private String buildExcerpt(String text) {
        String sanitized = MarkdownContentSanitizer.stripImages(text);
        if (StrUtil.isBlank(sanitized)) {
            return "未提供片段";
        }

        String normalized = sanitized.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_CITATION_EXCERPT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_CITATION_EXCERPT_LENGTH).trim() + "...";
    }

    private String mergePrompt(String template, String contract) {
        if (StrUtil.isBlank(template)) {
            return contract;
        }
        if (StrUtil.isBlank(contract)) {
            return template;
        }
        return template + "\n\n" + contract;
    }

    private String renderSection(String section, Map<String, String> slots) {
        return promptTemplateLoader.renderSection(RAGConstant.CONTEXT_FORMAT_PATH, section, slots);
    }

    private String nodeKey(IntentNode node) {
        if (node == null) {
            return "";
        }
        if (StrUtil.isNotBlank(node.getId())) {
            return node.getId();
        }
        if (StrUtil.isNotBlank(node.getIntentCode())) {
            return node.getIntentCode().trim();
        }
        if (StrUtil.isNotBlank(node.getCollectionName())) {
            return node.getCollectionName().trim();
        }
        return StrUtil.blankToDefault(node.getName(), "").trim();
    }
}


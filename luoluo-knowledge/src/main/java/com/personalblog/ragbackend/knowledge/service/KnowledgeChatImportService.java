package com.personalblog.ragbackend.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalblog.ragbackend.common.context.UserContext;
import com.personalblog.ragbackend.core.chunk.ChunkingMode;
import com.personalblog.ragbackend.knowledge.controller.request.ChatImportRequest;
import com.personalblog.ragbackend.knowledge.controller.vo.ChatImportSummaryVO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeBaseDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentDO;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatMessage;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import com.personalblog.ragbackend.knowledge.domain.enums.DocumentStatus;
import com.personalblog.ragbackend.knowledge.domain.enums.ProcessMode;
import com.personalblog.ragbackend.knowledge.domain.enums.SourceType;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentMapper;
import com.personalblog.ragbackend.knowledge.service.chat.ChatChunkingOptions;
import com.personalblog.ragbackend.knowledge.service.chat.ChatTranscriptParser;
import com.personalblog.ragbackend.knowledge.service.chat.ChatTranscriptParserRegistry;
import com.personalblog.ragbackend.knowledge.service.document.KnowledgeFileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识对话导入服务
 */
@Service
public class KnowledgeChatImportService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeBaseAccessService knowledgeBaseAccessService;
    private final KnowledgeFileStorageService knowledgeFileStorageService;
    private final ChatTranscriptParserRegistry chatTranscriptParserRegistry;
    private final ObjectMapper objectMapper;

    public KnowledgeChatImportService(KnowledgeBaseMapper knowledgeBaseMapper,
                                      KnowledgeDocumentMapper knowledgeDocumentMapper,
                                      KnowledgeDocumentService knowledgeDocumentService,
                                      KnowledgeBaseAccessService knowledgeBaseAccessService,
                                      KnowledgeFileStorageService knowledgeFileStorageService,
                                      ChatTranscriptParserRegistry chatTranscriptParserRegistry,
                                      ObjectMapper objectMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.knowledgeBaseAccessService = knowledgeBaseAccessService;
        this.knowledgeFileStorageService = knowledgeFileStorageService;
        this.chatTranscriptParserRegistry = chatTranscriptParserRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatImportSummaryVO importQqChatTranscript(String kbId, ChatImportRequest request, MultipartFile file) {
        return importChatTranscript(kbId, request, file, "qq");
    }

    @Transactional
    public ChatImportSummaryVO importWechatChatTranscript(String kbId, ChatImportRequest request, MultipartFile file) {
        return importChatTranscript(kbId, request, file, "wechat");
    }

    private ChatImportSummaryVO importChatTranscript(String kbId, ChatImportRequest request, MultipartFile file, String platform) {
        KnowledgeBaseDO knowledgeBase = requireKnowledgeBase(parseId(kbId));
        knowledgeBaseAccessService.assertManageable(knowledgeBase);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("chat transcript file is required");
        }
        if (request != null && StringUtils.hasText(request.getSplitBy()) && !"month".equalsIgnoreCase(request.getSplitBy().trim())) {
            throw new IllegalArgumentException("chat import only supports splitBy=month");
        }

        ChatTranscriptParser parser = chatTranscriptParserRegistry.requireByPlatform(platform);
        QqChatTranscript transcript = parser.parse(file);
        if (transcript.messages().isEmpty()) {
            throw new IllegalArgumentException("chat transcript contains no messages");
        }

        String fileUrl = knowledgeFileStorageService.store(
                file,
                knowledgeBase.getCollectionName(),
                StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : platform + "-chat.txt"
        );
        String sourceFileHash = sha256Hex(file);

        ChatChunkingOptions options = normalizeOptions(request, transcript.messages().size());
        String chunkConfig = toJson(Map.of(
                "minMessages", options.minMessages(),
                "maxMessages", options.maxMessages(),
                "overlapMessages", options.overlapMessages(),
                "targetChars", options.targetChars(),
                "maxChars", options.maxChars(),
                "splitGapMinutes", options.splitGapMinutes()
        ));

        Set<String> months = collectMonths(transcript.messages());
        List<String> createdDocIds = new ArrayList<>(months.size());
        for (String month : months) {
            int monthMessageCount = (int) transcript.messages().stream()
                    .filter(message -> message.timestamp() != null && month.equals(YearMonth.from(message.timestamp()).toString()))
                    .count();
            if (monthMessageCount <= 0) {
                continue;
            }
            KnowledgeDocumentDO existing = knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentDO>()
                    .eq(KnowledgeDocumentDO::getKbId, knowledgeBase.getId())
                    .eq(KnowledgeDocumentDO::getDocName, buildDocumentName(transcript, month))
                    .eq(KnowledgeDocumentDO::getDeleted, 0)
                    .last("limit 1"));
            if (existing != null) {
                throw new IllegalArgumentException("chat month document already exists: " + month);
            }

            KnowledgeDocumentDO document = new KnowledgeDocumentDO();
            document.setKbId(knowledgeBase.getId());
            document.setDocName(buildDocumentName(transcript, month));
            document.setEnabled(1);
            document.setChunkCount(0);
            document.setFileUrl(fileUrl);
            document.setFileType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : "text/plain");
            document.setFileSize(file.getSize());
            document.setProcessMode(ProcessMode.CHUNK.getValue());
            document.setStatus(DocumentStatus.PENDING.getCode());
            document.setSourceType(SourceType.FILE.getValue());
            document.setSourceFileName(transcript.sourceFileName());
            document.setChunkStrategy(ChunkingMode.CHAT_QQ_WINDOW.getValue());
            document.setChunkConfig(chunkConfig);
            document.setMetadataJson(toJson(Map.of(
                    "docType", transcript.docType(),
                    "chatPlatform", transcript.platform(),
                    "groupName", transcript.groupName(),
                    "bucketMonth", month,
                    "sourceFileHash", sourceFileHash,
                    "sourceFileUrl", fileUrl,
                    "monthMessageCount", monthMessageCount,
                    "sourceFile", transcript.sourceFileName()
            )));
            document.setCreatedBy(parseUserId(UserContext.getUserId()));
            document.setUpdatedBy(parseUserId(UserContext.getUserId()));
            knowledgeDocumentMapper.insert(document);
            createdDocIds.add(String.valueOf(document.getId()));
        }

        if (resolveAutoStart(request) && !createdDocIds.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (String docId : createdDocIds) {
                        knowledgeDocumentService.startChunk(docId);
                    }
                }
            });
        }

        return new ChatImportSummaryVO(
                transcript.groupName(),
                fileUrl,
                createdDocIds.size(),
                List.copyOf(createdDocIds),
                List.copyOf(months)
        );
    }

    private Set<String> collectMonths(List<QqChatMessage> messages) {
        LinkedHashSet<String> months = new LinkedHashSet<>();
        if (messages == null) {
            return months;
        }
        for (QqChatMessage message : messages) {
            if (message == null || message.timestamp() == null) {
                continue;
            }
            months.add(YearMonth.from(message.timestamp()).toString());
        }
        return months;
    }

    private ChatChunkingOptions normalizeOptions(ChatImportRequest request, int totalMessages) {
        int minMessages = intValue(request == null ? null : request.getMinMessages(), 6);
        int maxMessages = intValue(request == null ? null : request.getMaxMessages(), 12);
        int overlapMessages = intValue(request == null ? null : request.getOverlapMessages(), 2);
        int targetChars = intValue(request == null ? null : request.getTargetChars(), 900);
        int maxChars = intValue(request == null ? null : request.getMaxChars(), 1200);
        int splitGapMinutes = intValue(request == null ? null : request.getSplitGapMinutes(), 30);
        int maxChunkCount = Math.min(6000, (int) Math.ceil(totalMessages / (double) Math.max(1, maxMessages - overlapMessages)) + 50);
        return new ChatChunkingOptions(minMessages, maxMessages, overlapMessages, targetChars, maxChars, splitGapMinutes, maxChunkCount);
    }

    private boolean resolveAutoStart(ChatImportRequest request) {
        return request == null || request.getAutoStart() == null || request.getAutoStart();
    }

    private int intValue(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String buildDocumentName(QqChatTranscript transcript, String month) {
        String fallback = StringUtils.hasText(transcript.platform()) ? transcript.platform().trim() + "-chat" : "chat";
        String normalizedGroupName = StringUtils.hasText(transcript.groupName()) ? transcript.groupName().trim() : fallback;
        if ("qq".equalsIgnoreCase(transcript.platform())) {
            return normalizedGroupName + "#" + month;
        }
        return normalizedGroupName + "#" + month + "@" + transcript.platform();
    }

    private KnowledgeBaseDO requireKnowledgeBase(Long kbId) {
        KnowledgeBaseDO entity = knowledgeBaseMapper.selectById(kbId);
        if (entity == null) {
            throw new IllegalArgumentException("knowledge base not found");
        }
        return entity;
    }

    private Long parseId(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return Long.valueOf(value.trim());
    }

    private Long parseUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            return Long.valueOf(userId.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String sha256Hex(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                String hex = Integer.toHexString(Byte.toUnsignedInt(value));
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash chat transcript file", exception);
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize chat import metadata", exception);
        }
    }
}

package com.personalblog.ragbackend.knowledge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personalblog.ragbackend.common.context.UserContext;
import com.personalblog.ragbackend.core.chunk.VectorChunk;
import com.personalblog.ragbackend.infra.embedding.EmbeddingService;
import com.personalblog.ragbackend.infra.token.TokenCounterService;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeDocumentPageRequest;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeDocumentUpdateRequest;
import com.personalblog.ragbackend.knowledge.controller.request.KnowledgeDocumentUploadRequest;
import com.personalblog.ragbackend.knowledge.controller.vo.KnowledgeDocumentChunkLogVO;
import com.personalblog.ragbackend.knowledge.controller.vo.KnowledgeChunkVO;
import com.personalblog.ragbackend.knowledge.controller.vo.KnowledgeDocumentSearchVO;
import com.personalblog.ragbackend.knowledge.controller.vo.KnowledgeDocumentVO;
import com.personalblog.ragbackend.core.chunk.ChunkingMode;
import com.personalblog.ragbackend.core.chunk.TextChunkingOptions;
import com.personalblog.ragbackend.knowledge.config.RagDocumentUploadProperties;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeBaseDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentChunkLogDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeDocumentDO;
import com.personalblog.ragbackend.knowledge.dao.entity.KnowledgeChunkDO;
import com.personalblog.ragbackend.ingestion.dao.entity.IngestionPipelineDO;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatMessage;
import com.personalblog.ragbackend.knowledge.dto.chat.QqChatTranscript;
import com.personalblog.ragbackend.knowledge.dto.document.DocumentChunk;
import com.personalblog.ragbackend.knowledge.dto.document.DocumentChunkResponse;
import com.personalblog.ragbackend.knowledge.dto.document.ParseResult;
import com.personalblog.ragbackend.knowledge.domain.enums.SourceType;
import com.personalblog.ragbackend.knowledge.domain.enums.ProcessMode;
import com.personalblog.ragbackend.knowledge.domain.enums.DocumentStatus;
import com.personalblog.ragbackend.knowledge.config.KnowledgeScheduleProperties;
import com.personalblog.ragbackend.knowledge.config.RagKnowledgeProcessingProperties;
import com.personalblog.ragbackend.core.parser.DocumentParser;
import com.personalblog.ragbackend.core.parser.DocumentParserSelector;
import com.personalblog.ragbackend.knowledge.handler.RemoteFileFetcher;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeChunkMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentChunkLogMapper;
import com.personalblog.ragbackend.knowledge.mapper.KnowledgeDocumentMapper;
import com.personalblog.ragbackend.ingestion.dao.mapper.IngestionPipelineMapper;
import com.personalblog.ragbackend.knowledge.mq.MessageWrapper;
import com.personalblog.ragbackend.knowledge.mq.event.KnowledgeDocumentChunkEvent;
import com.personalblog.ragbackend.knowledge.service.KnowledgeBaseAccessService;
import com.personalblog.ragbackend.knowledge.service.KnowledgeDocumentScheduleService;
import com.personalblog.ragbackend.knowledge.service.KnowledgeChunkService;
import com.personalblog.ragbackend.knowledge.service.KnowledgeDocumentService;
import com.personalblog.ragbackend.knowledge.service.chat.ChatTranscriptCacheService;
import com.personalblog.ragbackend.knowledge.service.chat.ChatChunkingOptions;
import com.personalblog.ragbackend.knowledge.service.chat.ChatTranscriptParser;
import com.personalblog.ragbackend.knowledge.service.chat.ChatTranscriptParserRegistry;
import com.personalblog.ragbackend.knowledge.service.chat.ChatTranscriptChunkService;
import com.personalblog.ragbackend.knowledge.service.document.KnowledgeDocumentChunkService;
import com.personalblog.ragbackend.knowledge.service.document.KnowledgeFileStorageService;
import com.personalblog.ragbackend.ingestion.domain.context.DocumentSource;
import com.personalblog.ragbackend.ingestion.domain.context.IngestionContext;
import com.personalblog.ragbackend.ingestion.domain.context.NodeLog;
import com.personalblog.ragbackend.ingestion.domain.enums.IngestionStatus;
import com.personalblog.ragbackend.ingestion.domain.pipeline.PipelineDefinition;
import com.personalblog.ragbackend.ingestion.engine.IngestionEngine;
import com.personalblog.ragbackend.ingestion.service.IngestionPipelineService;
import com.personalblog.ragbackend.knowledge.service.vector.KnowledgeVectorSpaceResolver;
import com.personalblog.ragbackend.knowledge.service.vector.VectorStoreService;
import com.personalblog.ragbackend.knowledge.schedule.CronScheduleHelper;
import com.personalblog.ragbackend.rag.dto.StoredFileDTO;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识文档服务实现
 */
@Service
@Slf4j
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {
    @Value("knowledge-document-chunk_topic${unique-name:}")
    private String chunkTopic;

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeDocumentChunkLogMapper knowledgeDocumentChunkLogMapper;
    private final IngestionPipelineMapper ingestionPipelineMapper;
    private final KnowledgeScheduleProperties scheduleProperties;
    private final IngestionPipelineService ingestionPipelineService;
    private final IngestionEngine ingestionEngine;
    private final DocumentParserSelector documentParserSelector;
    private final KnowledgeDocumentChunkService knowledgeDocumentChunkService;
    private final KnowledgeVectorSpaceResolver vectorSpaceResolver;
    private final KnowledgeFileStorageService knowledgeFileStorageService;
    private final KnowledgeDocumentScheduleService knowledgeDocumentScheduleService;
    private final KnowledgeChunkService knowledgeChunkService;
    private final KnowledgeBaseAccessService knowledgeBaseAccessService;
    private final ChatTranscriptCacheService chatTranscriptCacheService;
    private final ChatTranscriptParserRegistry chatTranscriptParserRegistry;
    private final ChatTranscriptChunkService chatTranscriptChunkService;
    private final VectorStoreService vectorStoreService;
    private final EmbeddingService embeddingService;
    private final TokenCounterService tokenCounterService;
    private final TransactionOperations transactionOperations;
    private final ObjectMapper objectMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final RemoteFileFetcher remoteFileFetcher;
    private final RagKnowledgeProcessingProperties processingProperties;
    private final RagDocumentUploadProperties uploadProperties;

    public KnowledgeDocumentServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                        KnowledgeDocumentMapper knowledgeDocumentMapper,
                                        KnowledgeChunkMapper knowledgeChunkMapper,
                                        KnowledgeDocumentChunkLogMapper knowledgeDocumentChunkLogMapper,
                                        IngestionPipelineMapper ingestionPipelineMapper,
                                        KnowledgeScheduleProperties scheduleProperties,
                                        IngestionPipelineService ingestionPipelineService,
                                        IngestionEngine ingestionEngine,
                                        DocumentParserSelector documentParserSelector,
                                        KnowledgeDocumentChunkService knowledgeDocumentChunkService,
                                        KnowledgeVectorSpaceResolver vectorSpaceResolver,
                                        KnowledgeFileStorageService knowledgeFileStorageService,
                                        KnowledgeDocumentScheduleService knowledgeDocumentScheduleService,
                                        KnowledgeChunkService knowledgeChunkService,
                                        KnowledgeBaseAccessService knowledgeBaseAccessService,
                                        ChatTranscriptCacheService chatTranscriptCacheService,
                                        ChatTranscriptParserRegistry chatTranscriptParserRegistry,
                                        ChatTranscriptChunkService chatTranscriptChunkService,
                                        VectorStoreService vectorStoreService,
                                        EmbeddingService embeddingService,
                                        TokenCounterService tokenCounterService,
                                        TransactionOperations transactionOperations,
                                        ObjectMapper objectMapper,
                                        RocketMQTemplate rocketMQTemplate,
                                        RemoteFileFetcher remoteFileFetcher,
                                        RagKnowledgeProcessingProperties processingProperties,
                                        RagDocumentUploadProperties uploadProperties) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeDocumentChunkLogMapper = knowledgeDocumentChunkLogMapper;
        this.ingestionPipelineMapper = ingestionPipelineMapper;
        this.scheduleProperties = scheduleProperties;
        this.ingestionPipelineService = ingestionPipelineService;
        this.ingestionEngine = ingestionEngine;
        this.documentParserSelector = documentParserSelector;
        this.knowledgeDocumentChunkService = knowledgeDocumentChunkService;
        this.vectorSpaceResolver = vectorSpaceResolver;
        this.knowledgeFileStorageService = knowledgeFileStorageService;
        this.knowledgeDocumentScheduleService = knowledgeDocumentScheduleService;
        this.knowledgeChunkService = knowledgeChunkService;
        this.knowledgeBaseAccessService = knowledgeBaseAccessService;
        this.chatTranscriptCacheService = chatTranscriptCacheService;
        this.chatTranscriptParserRegistry = chatTranscriptParserRegistry;
        this.chatTranscriptChunkService = chatTranscriptChunkService;
        this.vectorStoreService = vectorStoreService;
        this.embeddingService = embeddingService;
        this.tokenCounterService = tokenCounterService;
        this.transactionOperations = transactionOperations;
        this.objectMapper = objectMapper;
        this.rocketMQTemplate = rocketMQTemplate;
        this.remoteFileFetcher = remoteFileFetcher;
        this.processingProperties = processingProperties;
        this.uploadProperties = uploadProperties;
    }

    @Override
    @Transactional
    public KnowledgeDocumentVO upload(String kbId, KnowledgeDocumentUploadRequest requestParam, MultipartFile file) {
        KnowledgeBaseDO knowledgeBase = requireKnowledgeBase(parseId(kbId));
        knowledgeBaseAccessService.assertManageable(knowledgeBase);

        SourceType sourceType = normalizeSourceType(requestParam.getSourceType());
        validateSourceAndSchedule(sourceType, requestParam.getSourceLocation(), requestParam.getScheduleEnabled(), requestParam.getScheduleCron());
        if (sourceType == SourceType.FILE && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("file is required");
        }
        ProcessMode processMode = normalizeProcessMode(requestParam.getProcessMode());
        validateUploadSize(file, sourceType, processMode);
        log.info(
                "Uploading knowledge document: kbId={}, kbName='{}', collection='{}', sourceType='{}', processMode='{}', filename='{}', fileSize={}, pipelineId='{}', chunkStrategy='{}'",
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getCollectionName(),
                sourceType.getValue(),
                processMode.getValue(),
                file == null ? null : file.getOriginalFilename(),
                file == null ? null : file.getSize(),
                requestParam.getPipelineId(),
                requestParam.getChunkStrategy()
        );
        String chunkStrategy = null;
        String chunkConfig = null;
        Long pipelineId = null;
        if (ProcessMode.PIPELINE == processMode) {
            if (!StringUtils.hasText(requestParam.getPipelineId())) {
                throw new IllegalArgumentException("pipeline id is required");
            }
            ingestionPipelineService.get(requestParam.getPipelineId());
            pipelineId = parseLong(requestParam.getPipelineId());
        } else {
            chunkStrategy = normalizeChunkStrategy(requestParam.getChunkStrategy());
            chunkConfig = blankToNull(requestParam.getChunkConfig());
        }
        StoredFileDTO storedFile;
        try {
            storedFile = storeUploadedFile(knowledgeBase.getCollectionName(), requestParam, file);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to store uploaded knowledge document: kbId={}, kbName='{}', collection='{}', sourceType='{}', processMode='{}', filename='{}'",
                    knowledgeBase.getId(),
                    knowledgeBase.getName(),
                    knowledgeBase.getCollectionName(),
                    sourceType.getValue(),
                    processMode.getValue(),
                    file == null ? null : file.getOriginalFilename(),
                    exception
            );
            throw exception;
        }
        KnowledgeDocumentDO entity = new KnowledgeDocumentDO();
        entity.setKbId(knowledgeBase.getId());
        entity.setDocName(StringUtils.hasText(storedFile.getOriginalFilename()) ? storedFile.getOriginalFilename() : resolveDocName(file));
        entity.setEnabled(1);
        entity.setChunkCount(0);
        entity.setFileUrl(storedFile.getUrl());
        entity.setFileType(storedFile.getDetectedType());
        entity.setFileSize(storedFile.getSize());
        entity.setProcessMode(processMode.getValue());
        entity.setStatus(DocumentStatus.PENDING.getCode());
        entity.setSourceType(sourceType.getValue());
        entity.setSourceLocation(sourceType == SourceType.URL ? blankToNull(requestParam.getSourceLocation()) : null);
        entity.setScheduleEnabled(sourceType == SourceType.URL && Boolean.TRUE.equals(requestParam.getScheduleEnabled()) ? 1 : 0);
        entity.setScheduleCron(sourceType == SourceType.URL && Boolean.TRUE.equals(requestParam.getScheduleEnabled())
                ? blankToNull(requestParam.getScheduleCron())
                : null);
        entity.setChunkStrategy(chunkStrategy);
        entity.setChunkConfig(chunkConfig);
        entity.setPipelineId(pipelineId);
        entity.setCreatedBy(parseUserId(UserContext.getUserId()));
        entity.setUpdatedBy(parseUserId(UserContext.getUserId()));
        log.info(
                "About to persist knowledge document: name='{}', sourceType='{}', processMode='{}', chunkStrategy='{}', chunkConfig='{}', pipelineId={}",
                entity.getDocName(),
                entity.getSourceType(),
                entity.getProcessMode(),
                entity.getChunkStrategy(),
                entity.getChunkConfig(),
                entity.getPipelineId()
        );
        try {
            knowledgeDocumentMapper.insert(entity);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to insert knowledge document row: kbId={}, kbName='{}', docName='{}', chunkStrategy='{}', chunkConfig='{}', pipelineId={}",
                    knowledgeBase.getId(),
                    knowledgeBase.getName(),
                    entity.getDocName(),
                    entity.getChunkStrategy(),
                    entity.getChunkConfig(),
                    entity.getPipelineId(),
                    exception
            );
            throw exception;
        }
        log.info(
                "Knowledge document uploaded successfully: kbId={}, docId={}, docName='{}', fileUrl='{}', processMode='{}'",
                knowledgeBase.getId(),
                entity.getId(),
                entity.getDocName(),
                entity.getFileUrl(),
                entity.getProcessMode()
        );
        return get(String.valueOf(entity.getId()));
    }

    @Override
    @Transactional
    public void startChunk(String docId) {
        KnowledgeDocumentDO document = requireDocument(parseId(docId));
        String operator = StringUtils.hasText(UserContext.getUsername()) ? UserContext.getUsername() : "system";
        if (!tryStartChunk(document, operator)) {
            throw new IllegalArgumentException("document is already running");
        }
    }

    @Override
    public int startChunkByKnowledgeBase(String kbId) {
        KnowledgeBaseDO knowledgeBase = requireKnowledgeBase(parseId(kbId));
        knowledgeBaseAccessService.assertManageable(knowledgeBase);
        List<KnowledgeDocumentDO> documents = knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocumentDO>()
                        .eq(KnowledgeDocumentDO::getKbId, knowledgeBase.getId())
                        .eq(KnowledgeDocumentDO::getDeleted, 0)
                        .eq(KnowledgeDocumentDO::getEnabled, 1)
                        .ne(KnowledgeDocumentDO::getStatus, DocumentStatus.RUNNING.getCode())
                        .orderByAsc(KnowledgeDocumentDO::getId)
        );
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        String operator = StringUtils.hasText(UserContext.getUsername()) ? UserContext.getUsername() : "system";
        int startedCount = 0;
        for (KnowledgeDocumentDO document : documents) {
            if (tryStartChunk(document, operator)) {
                startedCount++;
            }
        }
        return startedCount;
    }

    @Override
    public void executeChunk(String docId) {
        KnowledgeDocumentDO document = knowledgeDocumentMapper.selectById(parseId(docId));
        if (document == null) {
            log.warn("document not found, skip chunk task, docId={}", docId);
            return;
        }
        runChunkTask(document);
    }

    private record ChunkProcessResult(List<VectorChunk> chunks, long extractDuration, long chunkDuration,
                                      long embedDuration) {
    }

    private void runChunkTask(KnowledgeDocumentDO document) {
        String docId = String.valueOf(document.getId());
        ProcessMode processMode = normalizeProcessMode(document.getProcessMode());
        KnowledgeDocumentChunkLogDO chunkLog = insertChunkLog(document);
        log.info("document chunk task started, docId={}, kbId={}, processMode={}, fileName={}",
                docId, document.getKbId(), processMode, document.getDocName());

        long totalStartTime = System.currentTimeMillis();
        long extractDuration = 0L;
        long chunkDuration = 0L;
        long embedDuration = 0L;
        long persistDuration = 0L;

        try {
            List<VectorChunk> chunkResults;
            if (ProcessMode.PIPELINE == processMode) {
                ChunkProcessResult result = runPipelineProcess(document);
                chunkResults = result.chunks();
                extractDuration = result.extractDuration();
                chunkDuration = result.chunkDuration();
                embedDuration = result.embedDuration();
            } else {
                ChunkProcessResult result = runChunkProcess(document);
                chunkResults = result.chunks();
                extractDuration = result.extractDuration();
                chunkDuration = result.chunkDuration();
                embedDuration = result.embedDuration();
            }

            long persistStart = System.currentTimeMillis();
            int savedCount = persistChunksAndVectorsAtomically(document, chunkResults);
            persistDuration = System.currentTimeMillis() - persistStart;

            long totalDuration = System.currentTimeMillis() - totalStartTime;
            updateChunkLog(chunkLog.getId(), DocumentStatus.SUCCESS.getCode(), savedCount, extractDuration, chunkDuration, embedDuration, persistDuration, totalDuration, null);
            log.info("document chunk task finished, docId={}, kbId={}, chunkCount={}, extractMs={}, chunkMs={}, embedMs={}, persistMs={}, totalMs={}",
                    docId, document.getKbId(), savedCount, extractDuration, chunkDuration, embedDuration, persistDuration, totalDuration);
        } catch (Exception exception) {
            log.error("document chunk task failed, docId={}", docId, exception);
            markChunkFailed(document.getId());
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            updateChunkLog(chunkLog.getId(), DocumentStatus.FAILED.getCode(), 0, extractDuration, chunkDuration, embedDuration, persistDuration, totalDuration, exception.getMessage());
        }
    }

    private ChunkProcessResult runChunkProcess(KnowledgeDocumentDO document) {
        MultipartFile file = knowledgeFileStorageService.restore(document.getFileUrl(), document.getDocName(), document.getFileType());
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("document file is unavailable");
        }
        if (ChunkingMode.CHAT_QQ_WINDOW == ChunkingMode.from(document.getChunkStrategy())) {
            return runChatChunkProcess(document, file);
        }

        long extractStart = System.currentTimeMillis();
        DocumentParser parser = documentParserSelector.select(file.getContentType(), document.getDocName());
        ParseResult parseResult;
        try (InputStream inputStream = file.getInputStream()) {
            parseResult = parser.parse(inputStream, document.getDocName(), file.getContentType());
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read document file", exception);
        }
        long extractDuration = System.currentTimeMillis() - extractStart;

        if (parseResult == null || !parseResult.success()) {
            throw new IllegalStateException(parseResult == null ? "document parsing failed" : parseResult.errorMessage());
        }

        TextChunkingOptions options = buildChunkingOptions(document);
        long chunkStart = System.currentTimeMillis();
        DocumentChunkResponse chunkResponse = knowledgeDocumentChunkService.chunkContent(
                parseResult.content(),
                parseResult.mimeType(),
                parseResult.metadata(),
                parseResult.contentLength(),
                options
        );
        long chunkDuration = System.currentTimeMillis() - chunkStart;

        if (chunkResponse == null || !chunkResponse.success()) {
            throw new IllegalStateException(chunkResponse == null ? "document chunking failed" : chunkResponse.errorMessage());
        }

        long embedStart = System.currentTimeMillis();
        List<VectorChunk> vectorChunks = buildVectorChunksInBatches(
                document,
                chunkResponse.chunks(),
                Math.max(1, processingProperties.getEmbeddingBatchSize())
        );
        long embedDuration = System.currentTimeMillis() - embedStart;
        return new ChunkProcessResult(vectorChunks, extractDuration, chunkDuration, embedDuration);
    }

    private ChunkProcessResult runChatChunkProcess(KnowledgeDocumentDO document, MultipartFile file) {
        long extractStart = System.currentTimeMillis();
        Map<String, Object> documentMetadata = readJsonMap(document.getMetadataJson());
        ChatTranscriptParser parser = resolveChatTranscriptParser(documentMetadata);
        String bucketMonth = stringValue(documentMetadata, "bucketMonth");
        if (!StringUtils.hasText(bucketMonth)) {
            throw new IllegalStateException("chat transcript document is missing bucketMonth metadata");
        }
        byte[] fileBytes = getBytes(file);
        String sourceFileName = stringValue(documentMetadata, "sourceFile");
        if (!StringUtils.hasText(sourceFileName)) {
            sourceFileName = document.getDocName();
        }
        String sourceFileHash = stringValue(documentMetadata, "sourceFileHash");
        String cacheKey = StringUtils.hasText(sourceFileHash) ? sourceFileHash : document.getFileUrl();
        ChatTranscriptCacheService.CachedTranscript cachedTranscript = chatTranscriptCacheService.getOrParse(
                cacheKey,
                fileBytes,
                sourceFileName,
                parser
        );
        QqChatTranscript transcript = cachedTranscript.transcript();
        List<QqChatMessage> monthlyMessages = cachedTranscript.monthlyMessages(bucketMonth);
        long extractDuration = System.currentTimeMillis() - extractStart;
        if (monthlyMessages.isEmpty()) {
            throw new IllegalStateException("chat transcript contains no messages for month " + bucketMonth);
        }

        ChatChunkingOptions options = buildChatChunkingOptions(document, documentMetadata);
        long chunkStart = System.currentTimeMillis();
        List<DocumentChunk> chatChunks = chatTranscriptChunkService.chunkTranscript(transcript, bucketMonth, monthlyMessages, options);
        long chunkDuration = System.currentTimeMillis() - chunkStart;
        if (chatChunks.isEmpty()) {
            throw new IllegalStateException("chat transcript chunking produced no chunks");
        }

        long embedStart = System.currentTimeMillis();
        List<VectorChunk> vectorChunks = buildVectorChunksInBatches(
                document,
                chatChunks,
                Math.max(1, processingProperties.getChatEmbeddingBatchSize())
        );
        long embedDuration = System.currentTimeMillis() - embedStart;
        return new ChunkProcessResult(vectorChunks, extractDuration, chunkDuration, embedDuration);
    }

    private ChatTranscriptParser resolveChatTranscriptParser(Map<String, Object> documentMetadata) {
        String docType = stringValue(documentMetadata, "docType");
        if (!StringUtils.hasText(docType)) {
            return chatTranscriptParserRegistry.requireByPlatform("qq");
        }
        return chatTranscriptParserRegistry.requireByDocType(docType);
    }

    private ChunkProcessResult runPipelineProcess(KnowledgeDocumentDO document) {
        String docId = String.valueOf(document.getId());
        if (document.getPipelineId() == null) {
            throw new IllegalStateException("Pipeline mode requires pipeline id: docId=" + docId);
        }

        KnowledgeBaseDO knowledgeBase = requireKnowledgeBase(document.getKbId());
        MultipartFile file = knowledgeFileStorageService.restore(document.getFileUrl(), document.getDocName(), document.getFileType());
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("document file is unavailable");
        }

        PipelineDefinition pipeline = ingestionPipelineService.getDefinition(String.valueOf(document.getPipelineId()));
        IngestionContext previewContext = IngestionContext.builder()
                .taskId(docId)
                .pipelineId(String.valueOf(document.getPipelineId()))
                .source(DocumentSource.builder()
                        .type(com.personalblog.ragbackend.ingestion.domain.enums.SourceType.FILE)
                        .location(document.getSourceLocation())
                        .fileName(document.getDocName())
                        .build())
                .rawBytes(getBytes(file))
                .mimeType(file.getContentType())
                .vectorSpaceId(com.personalblog.ragbackend.rag.core.vector.VectorSpaceId.builder()
                        .logicalName(knowledgeBase.getCollectionName())
                        .build())
                .status(IngestionStatus.RUNNING)
                .logs(new ArrayList<>())
                .metadata(new HashMap<>())
                .skipIndexerWrite(true)
                .build();

        IngestionContext result = ingestionEngine.execute(pipeline, previewContext);
        if (result.getStatus() == IngestionStatus.FAILED || result.getError() != null) {
            String message = result.getError() == null ? "pipeline chunking failed" : result.getError().getMessage();
            throw new IllegalStateException(message);
        }

        List<VectorChunk> vectorChunks = result.getChunks() == null ? List.of() : result.getChunks();
        if (vectorChunks.isEmpty()) {
            throw new IllegalStateException("pipeline chunking failed");
        }
        List<List<Float>> embeddings = vectorChunks.stream()
                .map(chunk -> toEmbeddingList(chunk.getEmbedding()))
                .toList();
        DocumentChunkResponse chunkResponse = DocumentChunkResponse.success(
                result.getMimeType(),
                result.getMetadata() == null ? Map.<String, String>of() : result.getMetadata().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue()))),
                result.getRawText() == null ? 0 : result.getRawText().length(),
                512,
                640,
                128,
                vectorChunks.stream()
                        .map(chunk -> new DocumentChunk(
                                chunk.getIndex() == null ? 0 : chunk.getIndex(),
                                chunk.getMetadata() != null && chunk.getMetadata().get("sectionTitle") != null
                                        ? String.valueOf(chunk.getMetadata().get("sectionTitle"))
                                        : null,
                                chunk.getContent(),
                                chunk.getContent() == null ? 0 : chunk.getContent().length(),
                                Boolean.TRUE.equals(chunk.getMetadata() == null ? null : chunk.getMetadata().get("overlapFromPrevious")),
                                chunk.getMetadata()
                        ))
                        .toList()
        );
        long extractDuration = resolveNodeDuration(result.getLogs(), "parser");
        long chunkDuration = resolveMetadataDuration(result.getMetadata(), "chunkDurationMs", resolveNodeDuration(result.getLogs(), "chunker"));
        long embedDuration = resolveMetadataDuration(result.getMetadata(), "embedDurationMs", chunkDuration);
        List<VectorChunk> vectorChunksWithEmbeddings = buildVectorChunks(document, chunkResponse.chunks(), embeddings);
        return new ChunkProcessResult(vectorChunksWithEmbeddings, extractDuration, chunkDuration, embedDuration);
    }

    private KnowledgeDocumentChunkLogDO insertChunkLog(KnowledgeDocumentDO document) {
        KnowledgeDocumentChunkLogDO entity = new KnowledgeDocumentChunkLogDO();
        entity.setDocId(document.getId());
        entity.setStatus(DocumentStatus.RUNNING.getCode());
        entity.setProcessMode(document.getProcessMode());
        entity.setChunkStrategy(document.getChunkStrategy());
        entity.setPipelineId(document.getPipelineId());
        entity.setStartedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentChunkLogMapper.insert(entity);
        return entity;
    }

    private void updateChunkLog(Long logId,
                                String status,
                                int chunkCount,
                                long extractDuration,
                                long chunkDuration,
                                long embedDuration,
                                long persistDuration,
                                long totalDuration,
                                String errorMessage) {
        KnowledgeDocumentChunkLogDO entity = new KnowledgeDocumentChunkLogDO();
        entity.setId(logId);
        entity.setStatus(status);
        entity.setChunkCount(chunkCount);
        entity.setExtractDuration(extractDuration);
        entity.setChunkDuration(chunkDuration);
        entity.setEmbedDuration(embedDuration);
        entity.setPersistDuration(persistDuration);
        entity.setTotalDuration(totalDuration);
        entity.setErrorMessage(errorMessage);
        entity.setEndedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentChunkLogMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String docId) {
        KnowledgeDocumentDO document = requireDocument(parseId(docId));
        knowledgeBaseAccessService.assertManageable(requireKnowledgeBase(document.getKbId()));
        if (DocumentStatus.RUNNING.getCode().equalsIgnoreCase(document.getStatus())) {
            throw new IllegalArgumentException("document is already running");
        }
        String fileUrl = document.getFileUrl();
        deleteDocumentArtifacts(document);
        knowledgeDocumentMapper.deleteById(document.getId());
        knowledgeDocumentChunkLogMapper.delete(new LambdaQueryWrapper<KnowledgeDocumentChunkLogDO>()
                .eq(KnowledgeDocumentChunkLogDO::getDocId, document.getId()));
        knowledgeDocumentScheduleService.deleteByDocId(String.valueOf(document.getId()));
        if (shouldDeleteStoredFile(fileUrl)) {
            deleteStoredFileQuietly(fileUrl);
        }
    }

    @Override
    public KnowledgeDocumentVO get(String docId) {
        KnowledgeDocumentDO document = requireDocument(parseId(docId));
        knowledgeBaseAccessService.assertReadable(requireKnowledgeBase(document.getKbId()));
        return toView(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String docId, KnowledgeDocumentUpdateRequest requestParam) {
        KnowledgeDocumentDO document = requireDocument(parseId(docId));
        knowledgeBaseAccessService.assertManageable(requireKnowledgeBase(document.getKbId()));
        if (DocumentStatus.RUNNING.getCode().equalsIgnoreCase(document.getStatus())) {
            throw new IllegalArgumentException("document is already running");
        }
        if (requestParam == null || !StringUtils.hasText(requestParam.getDocName())) {
            throw new IllegalArgumentException("document name is required");
        }

        SourceType sourceType = normalizeSourceType(document.getSourceType());
        String finalSourceLocation = requestParam.getSourceLocation() != null
                ? blankToNull(requestParam.getSourceLocation())
                : document.getSourceLocation();
        boolean finalScheduleEnabled = requestParam.getScheduleEnabled() != null
                ? requestParam.getScheduleEnabled()
                : document.getScheduleEnabled() != null && document.getScheduleEnabled() == 1;
        String finalScheduleCron = requestParam.getScheduleCron() != null
                ? blankToNull(requestParam.getScheduleCron())
                : document.getScheduleCron();
        validateSourceAndSchedule(sourceType, finalSourceLocation, finalScheduleEnabled, finalScheduleCron);

        if (StringUtils.hasText(requestParam.getDocName())) {
            document.setDocName(requestParam.getDocName().trim());
        }
        boolean scheduleChanged = false;
        if (requestParam.getProcessMode() != null) {
            ProcessMode processMode = normalizeProcessMode(requestParam.getProcessMode());
            document.setProcessMode(processMode.getValue());
            if (ProcessMode.PIPELINE == processMode) {
                if (!StringUtils.hasText(requestParam.getPipelineId())) {
                    throw new IllegalArgumentException("pipeline id is required");
                }
                ingestionPipelineService.get(requestParam.getPipelineId());
                document.setPipelineId(parseLong(requestParam.getPipelineId()));
                document.setChunkStrategy(null);
                document.setChunkConfig(null);
            } else {
                document.setChunkStrategy(requestParam.getChunkStrategy() == null ? document.getChunkStrategy() : normalizeChunkStrategy(requestParam.getChunkStrategy()));
                document.setChunkConfig(requestParam.getChunkConfig() == null ? document.getChunkConfig() : blankToNull(requestParam.getChunkConfig()));
                document.setPipelineId(null);
            }
        } else {
            if (requestParam.getChunkStrategy() != null) {
                document.setChunkStrategy(normalizeChunkStrategy(requestParam.getChunkStrategy()));
            }
            if (requestParam.getChunkConfig() != null) {
                document.setChunkConfig(blankToNull(requestParam.getChunkConfig()));
            }
            if (requestParam.getPipelineId() != null) {
                ingestionPipelineService.get(requestParam.getPipelineId());
                document.setPipelineId(parseLong(requestParam.getPipelineId()));
            }
        }
        if (requestParam.getSourceLocation() != null) {
            document.setSourceLocation(blankToNull(requestParam.getSourceLocation()));
            scheduleChanged = true;
        }
        if (requestParam.getScheduleEnabled() != null) {
            document.setScheduleEnabled(Boolean.TRUE.equals(requestParam.getScheduleEnabled()) ? 1 : 0);
            scheduleChanged = true;
        }
        if (requestParam.getScheduleCron() != null) {
            document.setScheduleCron(blankToNull(requestParam.getScheduleCron()));
            scheduleChanged = true;
        }
        document.setUpdatedBy(parseUserId(UserContext.getUserId()));
        knowledgeDocumentMapper.updateById(document);
        if (scheduleChanged) {
            knowledgeDocumentScheduleService.upsertSchedule(document);
        }
    }

    @Override
    public IPage<KnowledgeDocumentVO> page(String kbId, KnowledgeDocumentPageRequest requestParam) {
        KnowledgeBaseDO knowledgeBase = requireKnowledgeBase(parseId(kbId));
        knowledgeBaseAccessService.assertReadable(knowledgeBase);
        Page<KnowledgeDocumentDO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<KnowledgeDocumentDO> result = knowledgeDocumentMapper.selectPage(
                page,
                new LambdaQueryWrapper<KnowledgeDocumentDO>()
                        .eq(KnowledgeDocumentDO::getKbId, knowledgeBase.getId())
                        .eq(KnowledgeDocumentDO::getDeleted, 0)
                        .like(StringUtils.hasText(requestParam.getKeyword()), KnowledgeDocumentDO::getDocName, requestParam.getKeyword())
                        .eq(StringUtils.hasText(requestParam.getStatus()), KnowledgeDocumentDO::getStatus, requestParam.getStatus())
                        .orderByDesc(KnowledgeDocumentDO::getCreatedAt)
        );
        return result.convert(this::toView);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(String docId, boolean enabled) {
        KnowledgeDocumentDO document = requireDocument(parseId(docId));
        knowledgeBaseAccessService.assertManageable(requireKnowledgeBase(document.getKbId()));
        if (DocumentStatus.RUNNING.getCode().equalsIgnoreCase(document.getStatus())) {
            throw new IllegalArgumentException("document is already running");
        }
        KnowledgeBaseDO knowledgeBase = requireKnowledgeBase(document.getKbId());
        int targetEnabled = enabled ? 1 : 0;
        if (document.getEnabled() != null && document.getEnabled() == targetEnabled) {
            return;
        }

        List<KnowledgeChunkVO> chunks = null;
        List<VectorChunk> vectorDocuments = null;
        if (enabled) {
            chunks = knowledgeChunkService.listByDocId(docId);
            if (chunks == null || chunks.isEmpty()) {
                log.warn("enable document skipped because no chunk found, docId={}", docId);
                return;
            }
            vectorDocuments = buildVectorChunks(
                    document,
                    chunks,
                    Math.max(1, processingProperties.getEmbeddingBatchSize())
            );
        }

        List<VectorChunk> finalVectorDocuments = vectorDocuments;
        document.setEnabled(enabled ? 1 : 0);
        knowledgeDocumentMapper.updateById(document);
        knowledgeChunkService.updateEnabledByDocId(docId, String.valueOf(knowledgeBase.getId()), enabled);
        if (!enabled) {
            deleteDocumentVectors(document);
        } else if (finalVectorDocuments != null && !finalVectorDocuments.isEmpty()) {
            vectorStoreService.indexDocumentChunks(knowledgeBase.getCollectionName(), String.valueOf(document.getId()), finalVectorDocuments);
        }
        knowledgeDocumentScheduleService.syncScheduleIfExists(document);
    }

    @Override
    public List<KnowledgeDocumentSearchVO> search(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        int size = Math.min(Math.max(limit, 1), 20);
        List<KnowledgeDocumentDO> records = knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocumentDO>()
                        .eq(KnowledgeDocumentDO::getDeleted, 0)
                        .like(StringUtils.hasText(keyword), KnowledgeDocumentDO::getDocName, keyword)
                        .orderByDesc(KnowledgeDocumentDO::getUpdatedAt)
        ).stream()
                .filter(document -> knowledgeBaseAccessService.canRead(requireKnowledgeBase(document.getKbId())))
                .limit(size)
                .toList();
        List<Long> kbIds = records.stream()
                .map(KnowledgeDocumentDO::getKbId)
                .distinct()
                .toList();
        Map<Long, String> kbNameMap = kbIds.isEmpty()
                ? Map.of()
                : knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBaseDO>()
                        .in(KnowledgeBaseDO::getId, kbIds))
                .stream()
                .collect(Collectors.toMap(KnowledgeBaseDO::getId, KnowledgeBaseDO::getName, (left, right) -> left));
        return records.stream().map(entity -> {
            KnowledgeDocumentSearchVO vo = BeanUtil.toBean(entity, KnowledgeDocumentSearchVO.class);
            vo.setId(String.valueOf(entity.getId()));
            vo.setKbId(String.valueOf(entity.getKbId()));
            vo.setKbName(kbNameMap.get(entity.getKbId()));
            return vo;
        }).toList();
    }

    @Override
    public IPage<KnowledgeDocumentChunkLogVO> getChunkLogs(String docId, Page<KnowledgeDocumentChunkLogVO> page) {
        KnowledgeDocumentDO document = requireDocument(parseId(docId));
        knowledgeBaseAccessService.assertManageable(requireKnowledgeBase(document.getKbId()));
        Page<KnowledgeDocumentChunkLogDO> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<KnowledgeDocumentChunkLogDO> result = knowledgeDocumentChunkLogMapper.selectPage(
                mpPage,
                new LambdaQueryWrapper<KnowledgeDocumentChunkLogDO>()
                        .eq(KnowledgeDocumentChunkLogDO::getDocId, document.getId())
                        .orderByDesc(KnowledgeDocumentChunkLogDO::getCreatedAt)
        );
        List<KnowledgeDocumentChunkLogDO> records = result.getRecords();
        Map<Long, String> pipelineNameMap = new HashMap<>();
        if (!records.isEmpty()) {
            Set<Long> pipelineIds = records.stream()
                    .map(KnowledgeDocumentChunkLogDO::getPipelineId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
            if (!pipelineIds.isEmpty()) {
                List<IngestionPipelineDO> pipelines = ingestionPipelineMapper.selectByIds(pipelineIds);
                for (IngestionPipelineDO pipeline : pipelines) {
                    pipelineNameMap.put(pipeline.id, pipeline.name);
                }
            }
        }

        Page<KnowledgeDocumentChunkLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records.stream().map(each -> {
            KnowledgeDocumentChunkLogVO vo = BeanUtil.toBean(each, KnowledgeDocumentChunkLogVO.class);
            vo.setId(String.valueOf(each.getId()));
            vo.setDocId(String.valueOf(each.getDocId()));
            vo.setPipelineId(each.getPipelineId() == null ? null : String.valueOf(each.getPipelineId()));
            vo.setDurationMs(each.getTotalDuration());
            vo.setMessage(each.getErrorMessage());
            vo.setRemark(each.getErrorMessage());
            if (each.getPipelineId() != null) {
                vo.setPipelineName(pipelineNameMap.get(each.getPipelineId()));
            }
            if (each.getTotalDuration() != null) {
                long otherDuration = "pipeline".equalsIgnoreCase(each.getProcessMode())
                        ? each.getTotalDuration() - (each.getChunkDuration() == null ? 0 : each.getChunkDuration()) - (each.getPersistDuration() == null ? 0 : each.getPersistDuration())
                        : each.getTotalDuration()
                        - (each.getExtractDuration() == null ? 0 : each.getExtractDuration())
                        - (each.getChunkDuration() == null ? 0 : each.getChunkDuration())
                        - (each.getEmbedDuration() == null ? 0 : each.getEmbedDuration())
                        - (each.getPersistDuration() == null ? 0 : each.getPersistDuration());
                vo.setOtherDuration(Math.max(0, otherDuration));
            }
            return vo;
        }).toList());
        return voPage;
    }

    private KnowledgeDocumentVO toView(KnowledgeDocumentDO entity) {
        KnowledgeDocumentVO vo = BeanUtil.toBean(entity, KnowledgeDocumentVO.class);
        vo.setId(String.valueOf(entity.getId()));
        vo.setKbId(String.valueOf(entity.getKbId()));
        vo.setPipelineId(entity.getPipelineId() == null ? null : String.valueOf(entity.getPipelineId()));
        vo.setCreatedBy(entity.getCreatedBy() == null ? null : String.valueOf(entity.getCreatedBy()));
        vo.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private void deleteDocumentArtifacts(KnowledgeDocumentDO document) {
        deleteDocumentVectors(document);
        knowledgeChunkService.deleteByDocId(String.valueOf(document.getId()));
    }

    private void deleteDocumentVectors(KnowledgeDocumentDO document) {
        KnowledgeBaseDO knowledgeBase = requireKnowledgeBase(document.getKbId());
        vectorStoreService.deleteDocumentVectors(knowledgeBase.getCollectionName(), String.valueOf(document.getId()));
    }

    private List<VectorChunk> buildVectorChunks(KnowledgeDocumentDO document,
                                                List<DocumentChunk> chunks,
                                                List<List<Float>> embeddings) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        if (embeddings == null || embeddings.size() != chunks.size()) {
            throw new IllegalStateException("embedding result size mismatch");
        }
        List<VectorChunk> vectorChunks = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            DocumentChunk chunk = chunks.get(index);
            vectorChunks.add(VectorChunk.builder()
                    .content(chunk.content())
                    .index(chunk.chunkIndex())
                    .metadata(buildVectorMetadata(document, chunk))
                    .embedding(toArray(embeddings.get(index)))
                    .build());
        }
        return vectorChunks;
    }

    private List<VectorChunk> buildVectorChunksInBatches(KnowledgeDocumentDO document,
                                                         List<DocumentChunk> chunks,
                                                         int batchSize) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        if (chunks.size() > batchSize) {
            log.info("embedding document chunks in batches, docId={}, totalChunks={}, batchSize={}",
                    document.getId(), chunks.size(), batchSize);
        }
        List<VectorChunk> vectorChunks = new ArrayList<>(chunks.size());
        for (int start = 0; start < chunks.size(); start += batchSize) {
            int end = Math.min(chunks.size(), start + batchSize);
            List<DocumentChunk> batch = chunks.subList(start, end);
            log.info("embedding document chunk batch, docId={}, batchStart={}, batchEnd={}, batchSize={}",
                    document.getId(), start, end - 1, batch.size());
            List<String> contents = new ArrayList<>(batch.size());
            for (DocumentChunk chunk : batch) {
                contents.add(chunk.content());
            }
            List<List<Float>> embeddings = embeddingService.embedBatch(contents);
            vectorChunks.addAll(buildVectorChunks(document, batch, embeddings));
        }
        return vectorChunks;
    }

    private List<VectorChunk> buildVectorChunks(KnowledgeDocumentDO document,
                                                List<KnowledgeChunkVO> chunks,
                                                int batchSize) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        if (chunks.size() > batchSize) {
            log.info("re-indexing document chunks in batches, docId={}, totalChunks={}, batchSize={}",
                    document.getId(), chunks.size(), batchSize);
        }
        List<VectorChunk> vectorChunks = new ArrayList<>(chunks.size());
        for (int start = 0; start < chunks.size(); start += batchSize) {
            int end = Math.min(chunks.size(), start + batchSize);
            List<KnowledgeChunkVO> batch = chunks.subList(start, end);
            log.info("re-indexing document chunk batch, docId={}, batchStart={}, batchEnd={}, batchSize={}",
                    document.getId(), start, end - 1, batch.size());
            List<String> contents = new ArrayList<>(batch.size());
            for (KnowledgeChunkVO chunk : batch) {
                contents.add(chunk.getContent());
            }
            List<List<Float>> embeddings = embeddingService.embedBatch(contents);
            if (embeddings == null || embeddings.size() != batch.size()) {
                throw new IllegalStateException("embedding result size mismatch");
            }
            for (int index = 0; index < batch.size(); index++) {
                KnowledgeChunkVO chunk = batch.get(index);
                vectorChunks.add(VectorChunk.builder()
                        .chunkId(chunk.getId())
                        .content(chunk.getContent())
                        .metadata(buildVectorMetadata(document, chunk))
                        .embedding(toArray(embeddings.get(index)))
                        .index(chunk.getChunkIndex())
                        .build());
            }
        }
        return vectorChunks;
    }

    private TextChunkingOptions buildChunkingOptions(KnowledgeDocumentDO document) {
        ChunkingMode chunkingMode = ChunkingMode.from(document.getChunkStrategy());
        if (ChunkingMode.CHAT_QQ_WINDOW == chunkingMode) {
            throw new IllegalStateException("chat documents must use chat chunking options");
        }
        Map<String, Object> chunkConfig = readChunkConfig(document.getChunkConfig());
        if (ChunkingMode.FIXED_SIZE == chunkingMode) {
            int chunkSize = intValue(chunkConfig, "chunkSize", 512);
            int overlapSize = intValue(chunkConfig, "overlapSize", 128);
            int maxChunkSize = Math.max(chunkSize, chunkSize + Math.max(0, overlapSize));
            return new TextChunkingOptions(chunkSize, maxChunkSize, overlapSize, 1000);
        }
        int targetChars = intValue(chunkConfig, "targetChars", 1400);
        int overlapChars = intValue(chunkConfig, "overlapChars", 0);
        int maxChars = intValue(chunkConfig, "maxChars", 1800);
        return new TextChunkingOptions(targetChars, Math.max(targetChars, maxChars), overlapChars, 1000);
    }

    private ChatChunkingOptions buildChatChunkingOptions(KnowledgeDocumentDO document, Map<String, Object> documentMetadata) {
        Map<String, Object> chunkConfig = readChunkConfig(document.getChunkConfig());
        int minMessages = intValue(chunkConfig, "minMessages", 6);
        int maxMessages = intValue(chunkConfig, "maxMessages", 12);
        int overlapMessages = intValue(chunkConfig, "overlapMessages", 2);
        int targetChars = intValue(chunkConfig, "targetChars", 900);
        int maxChars = intValue(chunkConfig, "maxChars", 1200);
        int splitGapMinutes = intValue(chunkConfig, "splitGapMinutes", 30);
        int monthMessageCount = intValue(documentMetadata, "monthMessageCount", 0);
        int maxChunkCount = resolveChatMaxChunkCount(monthMessageCount, maxMessages, overlapMessages);
        return new ChatChunkingOptions(
                minMessages,
                maxMessages,
                overlapMessages,
                targetChars,
                maxChars,
                splitGapMinutes,
                maxChunkCount
        );
    }

    private int resolveChatMaxChunkCount(int monthMessageCount, int maxMessages, int overlapMessages) {
        int effectiveWindow = Math.max(1, maxMessages - overlapMessages);
        int estimated = (int) Math.ceil(Math.max(0, monthMessageCount) / (double) effectiveWindow) + 50;
        return Math.min(6000, Math.max(50, estimated));
    }

    private void markChunkFailed(Long docId) {
        transactionOperations.executeWithoutResult(status -> {
            KnowledgeDocumentDO update = new KnowledgeDocumentDO();
            update.setId(docId);
            update.setStatus(DocumentStatus.FAILED.getCode());
            update.setUpdatedBy(parseUserId(UserContext.getUserId()));
            knowledgeDocumentMapper.updateById(update);
        });
    }

    private long resolveNodeDuration(List<NodeLog> nodeLogs, String nodeType) {
        if (nodeLogs == null || nodeLogs.isEmpty() || !StringUtils.hasText(nodeType)) {
            return 0L;
        }
        return nodeLogs.stream()
                .filter(each -> nodeType.equalsIgnoreCase(each.getNodeType()) || nodeType.equalsIgnoreCase(each.getNodeId()))
                .mapToLong(NodeLog::getDurationMs)
                .sum();
    }

    private long resolveMetadataDuration(Map<String, Object> metadata, String key, long fallback) {
        if (metadata == null || !metadata.containsKey(key)) {
            return fallback;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private List<Float> toEmbeddingList(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return List.of();
        }
        List<Float> values = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            values.add(value);
        }
        return values;
    }

    private byte[] getBytes(MultipartFile file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("read file bytes failed", exception);
        }
    }

    private Map<String, Object> readChunkConfig(String chunkConfig) {
        if (!StringUtils.hasText(chunkConfig)) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(chunkConfig, Map.class);
            return result == null ? Map.of() : result;
        } catch (Exception exception) {
            throw new IllegalArgumentException("chunk config is invalid", exception);
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            return result == null ? Map.of() : result;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private int intValue(Map<String, Object> config, String key, int defaultValue) {
        if (config == null || !config.containsKey(key) || config.get(key) == null) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String stringValue(Map<String, Object> config, String key) {
        if (config == null || !config.containsKey(key) || config.get(key) == null) {
            return null;
        }
        String value = String.valueOf(config.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    private Map<String, Object> mergeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(metadata);
    }

    private boolean shouldDeleteStoredFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return false;
        }
        Long remaining = knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentDO>()
                .eq(KnowledgeDocumentDO::getFileUrl, fileUrl)
                .eq(KnowledgeDocumentDO::getDeleted, 0));
        return remaining == null || remaining <= 0;
    }

    private int persistChunksAndVectorsAtomically(KnowledgeDocumentDO document,
                                                  List<VectorChunk> chunkResults) {
        KnowledgeBaseDO knowledgeBase = requireKnowledgeBase(document.getKbId());
        List<VectorChunk> safeChunks = chunkResults == null ? List.of() : chunkResults;
        log.info("persisting document chunks and vectors, docId={}, kbId={}, chunkCount={}, collection={}",
                document.getId(), document.getKbId(), safeChunks.size(), knowledgeBase.getCollectionName());
        try {
            transactionOperations.executeWithoutResult(status -> {
                log.info("cleaning old chunk data, docId={}", document.getId());
                knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunkDO>()
                        .eq(KnowledgeChunkDO::getDocId, document.getId()));

                log.info("cleaning old pg vectors, docId={}, collection={}", document.getId(), knowledgeBase.getCollectionName());
                vectorStoreService.deleteDocumentVectors(knowledgeBase.getCollectionName(), String.valueOf(document.getId()));

                for (VectorChunk chunk : safeChunks) {
                    KnowledgeChunkDO entity = new KnowledgeChunkDO();
                    Map<String, Object> metadata = mergeMetadata(chunk.getMetadata());
                    entity.setKbId(document.getKbId());
                    entity.setDocId(document.getId());
                    entity.setChunkIndex(chunk.getIndex());
                    entity.setContent(chunk.getContent());
                    entity.setContentHash(sha256Hex(chunk.getContent()));
                    entity.setCharCount(chunk.getContent() == null ? 0 : chunk.getContent().length());
                    entity.setTokenCount(resolveTokenCount(chunk.getContent()));
                    entity.setEnabled(1);
                    entity.setMetadata(toJson(metadata));
                    entity.setCreatedBy(parseUserId(UserContext.getUserId()));
                    entity.setUpdatedBy(parseUserId(UserContext.getUserId()));
                    knowledgeChunkMapper.insert(entity);
                    chunk.setChunkId(String.valueOf(entity.getId()));
                    metadata.put("chunkId", chunk.getChunkId());
                    metadata.put("chunk_id", chunk.getChunkId());
                    entity.setMetadata(toJson(metadata));
                    chunk.setMetadata(metadata);
                    knowledgeChunkMapper.updateById(entity);
                }

                document.setChunkCount(safeChunks.size());
                document.setUpdatedBy(parseUserId(UserContext.getUserId()));
                knowledgeDocumentMapper.updateById(document);
            });

            if (!safeChunks.isEmpty()) {
                log.info("indexing vectors, docId={}, collection={}, chunkCount={}", document.getId(), knowledgeBase.getCollectionName(), safeChunks.size());
                vectorStoreService.indexDocumentChunks(knowledgeBase.getCollectionName(), String.valueOf(document.getId()), safeChunks);
            }

            transactionOperations.executeWithoutResult(status -> {
                document.setStatus(DocumentStatus.SUCCESS.getCode());
                document.setUpdatedBy(parseUserId(UserContext.getUserId()));
                knowledgeDocumentMapper.updateById(document);
            });
            log.info("document chunk persistence done, docId={}, chunkCount={}", document.getId(), safeChunks.size());
            return safeChunks.size();
        } catch (RuntimeException exception) {
            cleanupPersistFailure(document, knowledgeBase);
            throw exception;
        }
    }

    private void cleanupPersistFailure(KnowledgeDocumentDO document, KnowledgeBaseDO knowledgeBase) {
        try {
            transactionOperations.executeWithoutResult(status -> {
                knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunkDO>()
                        .eq(KnowledgeChunkDO::getDocId, document.getId()));
                vectorStoreService.deleteDocumentVectors(knowledgeBase.getCollectionName(), String.valueOf(document.getId()));
                document.setChunkCount(0);
                document.setStatus(DocumentStatus.FAILED.getCode());
                document.setUpdatedBy(parseUserId(UserContext.getUserId()));
                knowledgeDocumentMapper.updateById(document);
            });
        } catch (RuntimeException cleanupException) {
            log.warn("cleanup after chunk persistence failure also failed, docId={}", document.getId(), cleanupException);
        }
    }

    private Map<String, Object> buildVectorMetadata(KnowledgeDocumentDO document, DocumentChunk chunk) {
        Map<String, Object> metadata = buildCommonVectorMetadata(document);
        metadata.putAll(mergeMetadata(chunk.metadata()));
        metadata.put("chunkIndex", chunk.chunkIndex());
        metadata.put("chunk_index", chunk.chunkIndex());
        metadata.put("sectionTitle", chunk.sectionTitle());
        metadata.put("contentLength", chunk.contentLength());
        metadata.put("overlapFromPrevious", chunk.overlapFromPrevious());
        return metadata;
    }

    private Map<String, Object> buildVectorMetadata(KnowledgeDocumentDO document, KnowledgeChunkVO chunk) {
        Map<String, Object> metadata = buildCommonVectorMetadata(document);
        metadata.putAll(readJsonMap(chunk.getMetadata()));
        metadata.put("chunkId", chunk.getId());
        metadata.put("chunk_id", chunk.getId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("chunk_index", chunk.getChunkIndex());
        metadata.putIfAbsent("sectionTitle", "");
        return metadata;
    }

    private Map<String, Object> buildCommonVectorMetadata(KnowledgeDocumentDO document) {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> documentMetadata = readJsonMap(document.getMetadataJson());
        String documentId = String.valueOf(document.getId());
        String knowledgeBaseId = String.valueOf(document.getKbId());
        metadata.putAll(documentMetadata);
        metadata.put("documentId", documentId);
        metadata.put("docId", documentId);
        metadata.put("doc_id", documentId);
        metadata.put("knowledgeBaseId", knowledgeBaseId);
        metadata.put("kbId", knowledgeBaseId);
        metadata.put("kb_id", knowledgeBaseId);
        metadata.put("baseCode", String.valueOf(document.getKbId()));
        metadata.put("title", document.getDocName());
        metadata.put("sourceUrl", document.getFileUrl());
        return metadata;
    }

    private String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                String hex = Integer.toHexString(Byte.toUnsignedInt(value));
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("failed to hash chunk content", exception);
        }
    }

    private Integer resolveTokenCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        Integer tokenCount = tokenCounterService.countTokens(content);
        return tokenCount == null ? 0 : tokenCount;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize metadata", exception);
        }
    }

    private void sendChunkMessage(Long documentId, String operator) {
        KnowledgeDocumentChunkEvent event = new KnowledgeDocumentChunkEvent(documentId, operator);
        MessageWrapper<KnowledgeDocumentChunkEvent> wrapper = new MessageWrapper<>();
        wrapper.setKeys(String.valueOf(documentId));
        wrapper.setBody(event);
        rocketMQTemplate.syncSend(
                chunkTopic,
                MessageBuilder.withPayload(wrapper)
                        .setHeader(MessageConst.PROPERTY_KEYS, String.valueOf(documentId))
                        .build()
        );
    }

    private boolean tryStartChunk(KnowledgeDocumentDO document, String operator) {
        int updated = knowledgeDocumentMapper.update(
                null,
                new LambdaUpdateWrapper<KnowledgeDocumentDO>()
                        .set(KnowledgeDocumentDO::getStatus, DocumentStatus.RUNNING.getCode())
                        .eq(KnowledgeDocumentDO::getId, document.getId())
                        .ne(KnowledgeDocumentDO::getStatus, DocumentStatus.RUNNING.getCode())
        );
        if (updated <= 0) {
            return false;
        }

        knowledgeDocumentScheduleService.upsertSchedule(document);
        Runnable sendTask = () -> sendChunkMessage(document.getId(), operator);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendTask.run();
                }
            });
            return true;
        }
        sendTask.run();
        return true;
    }

    private StoredFileDTO storeUploadedFile(String collectionName, KnowledgeDocumentUploadRequest requestParam, MultipartFile file) {
        log.info(
                "Preparing to store uploaded knowledge file: collection='{}', sourceType='{}', sourceLocation='{}', filename='{}'",
                collectionName,
                requestParam == null ? null : requestParam.getSourceType(),
                requestParam == null ? null : requestParam.getSourceLocation(),
                file == null ? null : file.getOriginalFilename()
        );
        if (requestParam != null && "url".equalsIgnoreCase(requestParam.getSourceType()) && StringUtils.hasText(requestParam.getSourceLocation())) {
            StoredFileDTO stored = remoteFileFetcher.fetchAndStore(collectionName, requestParam.getSourceLocation());
            if (stored.getOriginalFilename() == null) {
                stored.setOriginalFilename(resolveDocName(file));
            }
            return stored;
        }
        String originalFilename = resolveDocName(file);
        String url = knowledgeFileStorageService.store(file, collectionName, originalFilename);
        return StoredFileDTO.builder()
                .url(url)
                .detectedType(file == null ? null : file.getContentType())
                .size(file == null ? null : file.getSize())
                .originalFilename(originalFilename)
                .build();
    }

    private void validateUploadSize(MultipartFile file, SourceType sourceType, ProcessMode processMode) {
        if (file == null || file.isEmpty() || sourceType == SourceType.URL) {
            return;
        }
        long fileSize = Math.max(0L, file.getSize());
        long limitMb = processMode == ProcessMode.PIPELINE
                ? Math.max(1, uploadProperties.getMaxPipelineUploadSizeMb())
                : Math.max(1, uploadProperties.getMaxDocumentUploadSizeMb());
        long maxBytes = limitMb * 1024L * 1024L;
        if (fileSize > maxBytes) {
            throw new IllegalArgumentException("file too large, processMode="
                    + processMode.getValue()
                    + ", maxSizeMb="
                    + limitMb);
        }
    }

    private KnowledgeBaseDO requireKnowledgeBase(Long kbId) {
        KnowledgeBaseDO entity = knowledgeBaseMapper.selectById(kbId);
        if (entity == null) {
            throw new IllegalArgumentException("knowledge base not found");
        }
        return entity;
    }

    private KnowledgeDocumentDO requireDocument(Long docId) {
        KnowledgeDocumentDO entity = knowledgeDocumentMapper.selectById(docId);
        if (entity == null) {
            throw new IllegalArgumentException("document not found");
        }
        return entity;
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

    private Long parseId(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return Long.valueOf(value.trim());
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Long.valueOf(value.trim());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private SourceType normalizeSourceType(String value) {
        if (!StringUtils.hasText(value)) {
            return SourceType.FILE;
        }
        return SourceType.fromValue(value);
    }

    private ProcessMode normalizeProcessMode(String value) {
        return ProcessMode.fromValue(value);
    }

    private String normalizeChunkStrategy(String value) {
        return ChunkingMode.from(value).getValue();
    }

    private void validateSourceAndSchedule(SourceType sourceType, String sourceLocation, Boolean scheduleEnabled, String scheduleCron) {
        if (sourceType == SourceType.URL && !StringUtils.hasText(sourceLocation)) {
            throw new IllegalArgumentException("source location is required");
        }
        if (sourceType != SourceType.URL || !Boolean.TRUE.equals(scheduleEnabled)) {
            return;
        }
        if (!StringUtils.hasText(scheduleCron)) {
            throw new IllegalArgumentException("schedule cron is required");
        }
        try {
            if (CronScheduleHelper.isIntervalLessThan(scheduleCron, new Date(), scheduleProperties.getMinIntervalSeconds())) {
                throw new IllegalArgumentException("schedule interval is too short");
            }
        } catch (IllegalArgumentException exception) {
            if ("schedule interval is too short".equals(exception.getMessage())) {
                throw exception;
            }
            throw new IllegalArgumentException("schedule cron is invalid", exception);
        }
    }

    private String resolveDocName(MultipartFile file) {
        return file == null || !StringUtils.hasText(file.getOriginalFilename()) ? "uploaded-document" : file.getOriginalFilename().trim();
    }

    private void deleteStoredFileQuietly(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }
        try {
            knowledgeFileStorageService.deleteByUrl(fileUrl);
        } catch (Exception ignored) {
            // best effort cleanup
        }
    }

    private float[] toArray(List<Float> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return new float[0];
        }
        float[] values = new float[embedding.size()];
        for (int index = 0; index < embedding.size(); index++) {
            values[index] = embedding.get(index);
        }
        return values;
    }
}




package com.personalblog.ragbackend.infra.embedding;

import com.personalblog.ragbackend.infra.config.AIModelProperties;
import com.personalblog.ragbackend.infra.enums.ModelCapability;
import com.personalblog.ragbackend.infra.exception.RemoteException;
import com.personalblog.ragbackend.infra.model.ModelRoutingExecutor;
import com.personalblog.ragbackend.infra.model.ModelSelector;
import com.personalblog.ragbackend.infra.model.ModelTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Primary
public class RoutingEmbeddingService implements EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(RoutingEmbeddingService.class);

    private final ModelSelector selector;
    private final ModelRoutingExecutor executor;
    private final Map<String, EmbeddingClient> clientsByProvider;
    private final Semaphore embeddingSemaphore;

    public RoutingEmbeddingService(ModelSelector selector,
                                   ModelRoutingExecutor executor,
                                   AIModelProperties aiProperties,
                                   List<EmbeddingClient> clients) {
        this.selector = selector;
        this.executor = executor;
        this.clientsByProvider = clients.stream()
                .collect(Collectors.toMap(EmbeddingClient::provider, Function.identity()));
        int maxConcurrent = aiProperties.getEmbedding().getMaxConcurrent() == null
                ? 1
                : Math.max(1, aiProperties.getEmbedding().getMaxConcurrent());
        this.embeddingSemaphore = new Semaphore(maxConcurrent, true);
    }

    @Override
    public List<Float> embed(String text) {
        return withEmbeddingPermit("single", 1, () -> executor.executeWithFallback(
                ModelCapability.EMBEDDING,
                selector.selectEmbeddingCandidates(),
                target -> clientsByProvider.get(target.candidate().getProvider()),
                (client, target) -> client.embed(text, target)
        ));
    }

    @Override
    public List<Float> embed(String text, String modelId) {
        return withEmbeddingPermit("single", 1, () -> executor.executeWithFallback(
                ModelCapability.EMBEDDING,
                List.of(resolveTarget(modelId)),
                target -> clientsByProvider.get(target.candidate().getProvider()),
                (client, target) -> client.embed(text, target)
        ));
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        int batchSize = texts == null ? 0 : texts.size();
        return withEmbeddingPermit("batch", batchSize, () -> executor.executeWithFallback(
                ModelCapability.EMBEDDING,
                selector.selectEmbeddingCandidates(),
                target -> clientsByProvider.get(target.candidate().getProvider()),
                (client, target) -> client.embedBatch(texts, target)
        ));
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts, String modelId) {
        int batchSize = texts == null ? 0 : texts.size();
        return withEmbeddingPermit("batch", batchSize, () -> executor.executeWithFallback(
                ModelCapability.EMBEDDING,
                List.of(resolveTarget(modelId)),
                target -> clientsByProvider.get(target.candidate().getProvider()),
                (client, target) -> client.embedBatch(texts, target)
        ));
    }

    @Override
    public int dimension() {
        return selector.selectEmbeddingCandidates().stream()
                .map(ModelTarget::candidate)
                .map(candidate -> candidate.getDimension() == null ? 0 : candidate.getDimension())
                .filter(value -> value > 0)
                .findFirst()
                .orElse(0);
    }

    private ModelTarget resolveTarget(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            throw new RemoteException("Embedding model id must not be blank");
        }
        return selector.selectEmbeddingCandidates().stream()
                .filter(target -> modelId.equals(target.id()))
                .findFirst()
                .orElseThrow(() -> new RemoteException("Embedding model is unavailable: " + modelId));
    }

    private <T> T withEmbeddingPermit(String mode, int payloadSize, EmbeddingCall<T> call) {
        boolean acquired = false;
        try {
            log.info("embedding {} waiting for permit, payloadSize={}, availablePermits={}",
                    mode, payloadSize, embeddingSemaphore.availablePermits());
            embeddingSemaphore.acquire();
            acquired = true;
            log.info("embedding {} acquired permit, payloadSize={}, availablePermits={}",
                    mode, payloadSize, embeddingSemaphore.availablePermits());
            return call.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Embedding request interrupted while waiting for permit", exception, null);
        } finally {
            if (acquired) {
                embeddingSemaphore.release();
                log.info("embedding {} released permit, payloadSize={}, availablePermits={}",
                        mode, payloadSize, embeddingSemaphore.availablePermits());
            }
        }
    }

    @FunctionalInterface
    private interface EmbeddingCall<T> {
        T run();
    }
}

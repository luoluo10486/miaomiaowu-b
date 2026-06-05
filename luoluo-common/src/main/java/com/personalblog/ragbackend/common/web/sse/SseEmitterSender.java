package com.personalblog.ragbackend.common.web.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Thin wrapper around {@link SseEmitter} that swallows client-disconnect noise.
 */
public class SseEmitterSender {
    private static final Logger log = LoggerFactory.getLogger(SseEmitterSender.class);

    private final SseEmitter emitter;

    public SseEmitterSender(SseEmitter emitter) {
        this.emitter = emitter;
    }

    public void sendEvent(String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception exception) {
            if (isClientDisconnected(exception) || isEmitterAlreadyCompleted(exception)) {
                return;
            }
            throw new IllegalStateException("SSE event send failed: " + eventName, exception);
        }
    }

    public void complete() {
        try {
            emitter.complete();
        } catch (Exception exception) {
            if (!isClientDisconnected(exception)) {
                log.debug("SSE complete failed", exception);
            }
        }
    }

    public void fail(Throwable error) {
        try {
            emitter.completeWithError(error);
        } catch (Exception exception) {
            if (!isClientDisconnected(exception)) {
                log.debug("SSE completeWithError failed", exception);
            }
        }
    }

    private boolean isClientDisconnected(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String simpleName = current.getClass().getSimpleName();
            String message = current.getMessage();
            if ("ClientAbortException".equals(simpleName)
                    || "AsyncRequestNotUsableException".equals(simpleName)
                    || "EOFException".equals(simpleName)) {
                return true;
            }
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("broken pipe")
                        || normalized.contains("connection reset")
                        || normalized.contains("software caused connection abort")
                        || normalized.contains("宸插缓绔嬬殑杩炴帴")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isEmitterAlreadyCompleted(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("already completed")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

package com.personalblog.ragbackend.infra.model;

import com.personalblog.ragbackend.infra.config.AIModelProperties;

/**
 * 模型目标记录类
 */
public record ModelTarget(
        String id,
        AIModelProperties.ModelCandidate candidate,
        AIModelProperties.ProviderConfig provider
) {
}

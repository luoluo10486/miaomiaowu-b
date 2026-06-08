package com.personalblog.ragbackend.knowledge.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档状态
 */
@Getter
@RequiredArgsConstructor
public enum DocumentStatus {
    PENDING("pending"),
    RUNNING("running"),
    FAILED("failed"),
    SUCCESS("success");

    private final String code;
}

package com.personalblog.ragbackend.admin.controller.vo;

/**
 * Dashboard性能视图对象
 */
public record DashboardPerformanceVO(String window,
                                     Long avgLatencyMs,
                                     Long p95LatencyMs,
                                     Double successRate,
                                     Double errorRate,
                                     Double noDocRate,
                                     Double slowRate) {
}

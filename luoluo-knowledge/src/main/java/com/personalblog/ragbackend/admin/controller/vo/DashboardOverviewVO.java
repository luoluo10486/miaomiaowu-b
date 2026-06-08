package com.personalblog.ragbackend.admin.controller.vo;

/**
 * Dashboard概览视图对象
 */
public record DashboardOverviewVO(String window,
                                  String compareWindow,
                                  Long updatedAt,
                                  DashboardOverviewGroupVO kpis) {
}

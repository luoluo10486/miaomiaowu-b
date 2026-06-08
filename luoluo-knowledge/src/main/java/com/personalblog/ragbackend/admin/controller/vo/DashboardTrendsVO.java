package com.personalblog.ragbackend.admin.controller.vo;

import java.util.List;

/**
 * DashboardTrends视图对象
 */
public record DashboardTrendsVO(String metric,
                                String window,
                                String granularity,
                                List<DashboardTrendSeriesVO> series) {
}

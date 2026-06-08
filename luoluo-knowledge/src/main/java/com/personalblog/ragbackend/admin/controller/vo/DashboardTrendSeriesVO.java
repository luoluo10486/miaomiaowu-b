package com.personalblog.ragbackend.admin.controller.vo;

import java.util.List;

/**
 * Dashboard趋势序列视图对象
 */
public record DashboardTrendSeriesVO(String name, List<DashboardTrendPointVO> data) {
}

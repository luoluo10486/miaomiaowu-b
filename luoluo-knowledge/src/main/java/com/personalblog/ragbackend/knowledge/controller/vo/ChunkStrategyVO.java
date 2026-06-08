package com.personalblog.ragbackend.knowledge.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 分块Strategy视图对象
 */
@Data
@AllArgsConstructor
public class ChunkStrategyVO {
    private String value;
    private String label;
    private Map<String, Integer> defaultConfig;
}

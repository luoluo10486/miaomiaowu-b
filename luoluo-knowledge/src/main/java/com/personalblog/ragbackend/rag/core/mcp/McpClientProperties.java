package com.personalblog.ragbackend.rag.core.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP客户端配置属性
 */
@Data
@ConfigurationProperties(prefix = "rag.mcp")
public class McpClientProperties {
    private List<ServerConfig> servers = new ArrayList<>();

    @Data
    public static class ServerConfig {
        private String name;
        private String url;
    }
}

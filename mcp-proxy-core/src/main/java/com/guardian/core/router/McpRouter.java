package com.guardian.core.router;

import com.guardian.core.config.GuardianProperties;
import com.guardian.core.model.DownstreamServer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpRouter {

    private static final Logger log = LoggerFactory.getLogger(McpRouter.class);
    private final GuardianProperties properties;
    private final Map<String, DownstreamServer> serverRegistry = new ConcurrentHashMap<>();

    public McpRouter(GuardianProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (properties.servers() != null) {
            properties.servers().forEach((id, config) -> {
                if (config.enabled()) {
                    DownstreamServer server = config.toDownstreamServer(id);
                    serverRegistry.put(id, server);
                    log.info("Registered downstream MCP server: {} -> {}", id, config.url());
                }
            });
        }
        log.info("Router initialized with {} active servers", serverRegistry.size());
    }

    public Optional<DownstreamServer> resolve(String serverId) {
        return Optional.ofNullable(serverRegistry.get(serverId));
    }

    public Map<String, DownstreamServer> getAllServers() {
        return Map.copyOf(serverRegistry);
    }
}

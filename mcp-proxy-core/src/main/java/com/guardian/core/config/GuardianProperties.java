package com.guardian.core.config;

import com.guardian.core.model.DownstreamServer;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "guardian")
public record GuardianProperties(
        Map<String, ServerConfig> servers,
        ProxyConfig proxy
) {
    public record ServerConfig(
            String name,
            String url,
            String healthCheckUrl,
            boolean enabled
    ) {
        public DownstreamServer toDownstreamServer(String id) {
            return new DownstreamServer(id, name, url, healthCheckUrl, enabled);
        }
    }

    public record ProxyConfig(
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        public ProxyConfig {
            if (connectTimeoutMs <= 0) connectTimeoutMs = 5000;
            if (readTimeoutMs <= 0) readTimeoutMs = 30000;
        }
    }
}

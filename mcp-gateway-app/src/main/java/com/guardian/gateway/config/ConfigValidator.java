package com.guardian.gateway.config;

import com.guardian.core.config.GuardianProperties;
import com.guardian.gateway.security.JwtProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@Component
public class ConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    private final GuardianProperties guardianProperties;
    private final JwtProperties jwtProperties;
    private final Environment environment;

    public ConfigValidator(GuardianProperties guardianProperties,
                           JwtProperties jwtProperties,
                           Environment environment) {
        this.guardianProperties = guardianProperties;
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        log.info("=== MCP Guardian Configuration Validation ===");
        logActiveProfiles();
        validateProxyConfig();
        validateServerConfigs();
        validateJwtConfig();
        log.info("=== Configuration validation completed ===");
    }

    private void logActiveProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            log.info("Active profiles: [default]");
        } else {
            log.info("Active profiles: {}", Arrays.toString(profiles));
        }
    }

    private void validateProxyConfig() {
        var proxy = guardianProperties.proxy();
        if (proxy == null) {
            log.warn("Proxy configuration is missing, using defaults");
            return;
        }
        log.info("Proxy connect-timeout: {}ms, read-timeout: {}ms",
                proxy.connectTimeoutMs(), proxy.readTimeoutMs());

        if (proxy.connectTimeoutMs() > 30000) {
            log.warn("Proxy connect-timeout ({}ms) is unusually high", proxy.connectTimeoutMs());
        }
        if (proxy.readTimeoutMs() > 120000) {
            log.warn("Proxy read-timeout ({}ms) is unusually high", proxy.readTimeoutMs());
        }
    }

    private void validateServerConfigs() {
        var servers = guardianProperties.servers();
        if (servers == null || servers.isEmpty()) {
            log.warn("No downstream MCP servers configured");
            return;
        }

        int enabledCount = 0;
        for (var entry : servers.entrySet()) {
            String id = entry.getKey();
            var server = entry.getValue();

            if (!server.enabled()) {
                log.info("Server '{}': disabled", id);
                continue;
            }

            enabledCount++;

            // Validate URL format
            try {
                new URL(server.url());
                log.info("Server '{}' ({}): {}", id, server.name(), server.url());
            } catch (MalformedURLException e) {
                log.warn("Server '{}' has invalid URL: {}", id, server.url());
            }

            // Validate health check URL
            if (server.healthCheckUrl() != null && !server.healthCheckUrl().isEmpty()) {
                try {
                    new URL(server.healthCheckUrl());
                } catch (MalformedURLException e) {
                    log.warn("Server '{}' has invalid health-check URL: {}", id, server.healthCheckUrl());
                }
            }
        }

        if (enabledCount == 0) {
            log.warn("All downstream MCP servers are disabled");
        } else {
            log.info("Total enabled downstream servers: {}", enabledCount);
        }
    }

    private void validateJwtConfig() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            log.warn("JWT secret is too short (min 32 bytes required)");
        }

        // Warn if using default secret
        if (secret != null && secret.contains("default-secret")) {
            log.warn("JWT secret appears to be a default value - change it in production!");
        }

        // Log expiration (without exposing the secret)
        long expirationMs = jwtProperties.getExpirationMs();
        long expirationHours = expirationMs / 3600000;
        log.info("JWT token expiration: {}ms ({}h), secret length: {} bytes",
                expirationMs, expirationHours, secret != null ? secret.length() : 0);
    }
}

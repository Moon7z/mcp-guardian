package com.guardian.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(GuardianProperties.class)
public class ProxyCoreConfig {

    @Bean
    public WebClient mcpWebClient(GuardianProperties properties) {
        GuardianProperties.ProxyConfig proxy = properties.proxy();
        int connectTimeout = proxy != null ? proxy.connectTimeoutMs() : 5000;

        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}

package com.guardian.core.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(GuardianProperties.class)
public class ProxyCoreConfig {

    @Bean
    public WebClient mcpWebClient(GuardianProperties properties) {
        GuardianProperties.ProxyConfig proxy = properties.proxy();
        int connectTimeoutMs = proxy != null ? proxy.connectTimeoutMs() : 5000;
        int readTimeoutMs = proxy != null ? proxy.readTimeoutMs() : 30000;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}

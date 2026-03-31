package com.guardian.core.model;

public record DownstreamServer(
        String id,
        String name,
        String url,
        String healthCheckUrl,
        boolean enabled
) {
}

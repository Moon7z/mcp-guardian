package com.guardian.core.interceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InterceptorContext {

    private final String sessionId;
    private final String serverId;
    private final String userId;
    private final Instant timestamp;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public InterceptorContext(String sessionId, String serverId, String userId) {
        this.sessionId = sessionId;
        this.serverId = serverId;
        this.userId = userId;
        this.timestamp = Instant.now();
    }

    public String getSessionId() { return sessionId; }
    public String getServerId() { return serverId; }
    public String getUserId() { return userId; }
    public Instant getTimestamp() { return timestamp; }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}

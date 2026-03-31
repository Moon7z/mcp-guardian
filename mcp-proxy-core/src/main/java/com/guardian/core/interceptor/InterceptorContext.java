package com.guardian.core.interceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InterceptorContext {

    private final String sessionId;
    private final String serverId;
    private final String userId;
    private final String userRole;
    private final Instant timestamp;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public InterceptorContext(String sessionId, String serverId, String userId) {
        this(sessionId, serverId, userId, null);
    }

    public InterceptorContext(String sessionId, String serverId, String userId, String userRole) {
        this.sessionId = sessionId;
        this.serverId = serverId;
        this.userId = userId;
        this.userRole = userRole;
        this.timestamp = Instant.now();
    }

    public String getSessionId() { return sessionId; }
    public String getServerId() { return serverId; }
    public String getUserId() { return userId; }
    public String getUserRole() { return userRole != null ? userRole : userId; }
    public Instant getTimestamp() { return timestamp; }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new ClassCastException("Attribute '" + key + "' is " +
                value.getClass().getSimpleName() + ", not " + type.getSimpleName());
    }
}

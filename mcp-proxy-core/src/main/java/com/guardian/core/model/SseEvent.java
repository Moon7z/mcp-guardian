package com.guardian.core.model;

public record SseEvent(
        String id,
        String event,
        String data
) {
    public static SseEvent message(String id, String data) {
        return new SseEvent(id, "message", data);
    }

    public static SseEvent endpoint(String data) {
        return new SseEvent(null, "endpoint", data);
    }
}

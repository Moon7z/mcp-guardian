package com.guardian.audit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditRecord(
        String traceId,
        String sessionId,
        String userId,
        String serverId,
        String method,
        String paramsSummary,
        String resultSummary,
        boolean policyBlocked,
        String policyRule,
        boolean dlpRedacted,
        long durationMs,
        Instant timestamp
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String traceId;
        private String sessionId;
        private String userId;
        private String serverId;
        private String method;
        private String paramsSummary;
        private String resultSummary;
        private boolean policyBlocked;
        private String policyRule;
        private boolean dlpRedacted;
        private long durationMs;
        private Instant timestamp;

        public Builder traceId(String v) { this.traceId = v; return this; }
        public Builder sessionId(String v) { this.sessionId = v; return this; }
        public Builder userId(String v) { this.userId = v; return this; }
        public Builder serverId(String v) { this.serverId = v; return this; }
        public Builder method(String v) { this.method = v; return this; }
        public Builder paramsSummary(String v) { this.paramsSummary = v; return this; }
        public Builder resultSummary(String v) { this.resultSummary = v; return this; }
        public Builder policyBlocked(boolean v) { this.policyBlocked = v; return this; }
        public Builder policyRule(String v) { this.policyRule = v; return this; }
        public Builder dlpRedacted(boolean v) { this.dlpRedacted = v; return this; }
        public Builder durationMs(long v) { this.durationMs = v; return this; }
        public Builder timestamp(Instant v) { this.timestamp = v; return this; }

        public AuditRecord build() {
            return new AuditRecord(traceId, sessionId, userId, serverId, method,
                    paramsSummary, resultSummary, policyBlocked, policyRule,
                    dlpRedacted, durationMs, timestamp != null ? timestamp : Instant.now());
        }
    }
}

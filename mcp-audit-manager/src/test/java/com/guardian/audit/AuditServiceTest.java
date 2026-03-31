package com.guardian.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.audit.model.AuditRecord;
import com.guardian.audit.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditServiceTest {

    private final AuditService auditService = new AuditService(new ObjectMapper());

    @Test
    @DisplayName("Should log audit record without exception")
    void logAuditRecord() {
        var record = AuditRecord.builder()
                .traceId("trace-1")
                .sessionId("session-1")
                .userId("user-1")
                .serverId("db-tools")
                .method("tools/call")
                .paramsSummary("{\"tool\":\"query\"}")
                .resultSummary("{\"rows\":10}")
                .policyBlocked(false)
                .dlpRedacted(true)
                .durationMs(42)
                .timestamp(Instant.now())
                .build();

        assertDoesNotThrow(() -> auditService.logAsync(record));
    }

    @Test
    @DisplayName("Should keep recent records up to max")
    void recentRecords() {
        for (int i = 0; i < 5; i++) {
            auditService.logAsync(AuditRecord.builder()
                    .traceId("trace-" + i)
                    .sessionId("session-" + i)
                    .method("tools/call")
                    .durationMs(i * 10L)
                    .build());
        }
        assertEquals(5, auditService.getRecentRecords().size());
    }

    @Test
    @DisplayName("AuditRecord builder defaults timestamp")
    void builderDefaultTimestamp() {
        var record = AuditRecord.builder()
                .traceId("t1")
                .method("test")
                .build();
        assertNotNull(record.timestamp());
    }
}

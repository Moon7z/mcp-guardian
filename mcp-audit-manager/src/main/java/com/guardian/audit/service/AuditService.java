package com.guardian.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.audit.model.AuditRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

@Service
public class AuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final ObjectMapper objectMapper;
    private final LinkedBlockingDeque<AuditRecord> recentRecords = new LinkedBlockingDeque<>(MAX_RECENT);
    private static final int MAX_RECENT = 1000;

    public AuditService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Async("auditExecutor")
    public void logAsync(AuditRecord record) {
        try {
            String json = objectMapper.writeValueAsString(record);
            auditLog.info(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit record: {}", e.getMessage());
            auditLog.info("AUDIT|session={}|user={}|server={}|method={}|duration={}ms|blocked={}|redacted={}",
                    record.sessionId(), record.userId(), record.serverId(),
                    record.method(), record.durationMs(), record.policyBlocked(), record.dlpRedacted());
        }

        // Thread-safe bounded buffer: remove oldest if full
        while (!recentRecords.offerLast(record)) {
            recentRecords.pollFirst();
        }
    }

    public List<AuditRecord> getRecentRecords() {
        return new ArrayList<>(recentRecords);
    }
}

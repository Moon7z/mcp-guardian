package com.guardian.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.guardian.audit.model.AuditRecord;
import com.guardian.core.interceptor.InterceptorContext;
import com.guardian.core.interceptor.McpInterceptor;
import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class AuditInterceptor implements McpInterceptor {

    private final AuditService auditService;

    public AuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public int getOrder() {
        return 200; // post-handle runs in reverse order, so lower = runs after DLP
    }

    @Override
    public McpResponse postHandle(McpRequest request, McpResponse response, InterceptorContext context) {
        long durationMs = Duration.between(context.getTimestamp(), Instant.now()).toMillis();

        Boolean policyBlocked = context.getAttribute("policy.blocked");
        String policyRule = context.getAttribute("policy.rule");
        Boolean dlpRedacted = context.getAttribute("dlp.redacted");

        AuditRecord record = AuditRecord.builder()
                .traceId(UUID.randomUUID().toString())
                .sessionId(context.getSessionId())
                .userId(context.getUserId())
                .serverId(context.getServerId())
                .method(request.method())
                .paramsSummary(summarize(request.params()))
                .resultSummary(summarize(response.result()))
                .policyBlocked(policyBlocked != null && policyBlocked)
                .policyRule(policyRule)
                .dlpRedacted(dlpRedacted != null && dlpRedacted)
                .durationMs(durationMs)
                .build();

        auditService.logAsync(record);
        return response;
    }

    private String summarize(JsonNode node) {
        if (node == null) {
            return null;
        }
        String text = node.toString();
        if (text.length() > 200) {
            return text.substring(0, 200) + "...[truncated]";
        }
        return text;
    }
}

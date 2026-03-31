package com.guardian.dlp.filter;

import com.guardian.core.interceptor.InterceptorContext;
import com.guardian.core.interceptor.McpInterceptor;
import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DlpInterceptor implements McpInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DlpInterceptor.class);
    private final DlpRedactor redactor;

    public DlpInterceptor(DlpRedactor redactor) {
        this.redactor = redactor;
    }

    @Override
    public int getOrder() {
        return 300; // post-handle runs in reverse order, so higher = runs first
    }

    @Override
    public McpResponse postHandle(McpRequest request, McpResponse response, InterceptorContext context) {
        if (response == null || response.result() == null) {
            return response;
        }

        var redactedResult = redactor.redactJsonNode(response.result());
        boolean wasRedacted = !redactedResult.equals(response.result());

        context.setAttribute("dlp.redacted", wasRedacted);

        if (wasRedacted) {
            log.info("DLP filter applied to response for session={}, server={}",
                    context.getSessionId(), context.getServerId());
        }

        return McpResponse.success(response.id(), redactedResult);
    }
}

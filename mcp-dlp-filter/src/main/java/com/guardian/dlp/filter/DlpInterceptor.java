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
        return 300;
    }

    @Override
    public McpResponse postHandle(McpRequest request, McpResponse response, InterceptorContext context) {
        if (response == null) {
            return null;
        }

        if (response.result() == null) {
            context.setAttribute("dlp.redacted", false);
            return response;
        }

        var redactedResult = redactor.redactJsonNode(response.result());
        boolean wasRedacted = !redactedResult.equals(response.result());

        context.setAttribute("dlp.redacted", wasRedacted);

        if (wasRedacted) {
            log.info("DLP filter applied to response for session={}, server={}",
                    context.getSessionId(), context.getServerId());
        }

        // Preserve the error field if present
        return new McpResponse(response.jsonrpc(), response.id(), redactedResult, response.error());
    }
}

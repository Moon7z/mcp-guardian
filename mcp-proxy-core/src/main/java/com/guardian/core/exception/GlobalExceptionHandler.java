package com.guardian.core.exception;

import com.guardian.core.model.McpError;
import com.guardian.core.model.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(McpProxyException.class)
    public ResponseEntity<McpResponse> handleMcpProxyException(McpProxyException ex) {
        log.warn("MCP proxy error: {}", ex.getMessage());
        return ResponseEntity.ok(McpResponse.error(null, ex.getMcpError()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<McpResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.ok(McpResponse.error(null, McpError.internalError("Internal server error")));
    }
}

package com.guardian.core.exception;

import com.guardian.core.model.McpError;

public class McpProxyException extends RuntimeException {

    private final McpError mcpError;

    public McpProxyException(McpError mcpError) {
        super(mcpError.message());
        this.mcpError = mcpError;
    }

    public McpProxyException(McpError mcpError, Throwable cause) {
        super(mcpError.message(), cause);
        this.mcpError = mcpError;
    }

    public McpError getMcpError() {
        return mcpError;
    }

    public static McpProxyException serverUnreachable(String serverId, Throwable cause) {
        return new McpProxyException(
                McpError.serverUnreachable("Downstream server unreachable: " + serverId), cause);
    }

    public static McpProxyException policyViolation(String reason) {
        return new McpProxyException(McpError.invalidParams("Policy violation: " + reason));
    }
}

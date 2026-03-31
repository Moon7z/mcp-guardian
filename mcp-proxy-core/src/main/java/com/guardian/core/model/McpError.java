package com.guardian.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpError(
        @JsonProperty("code") int code,
        @JsonProperty("message") String message,
        @JsonProperty("data") JsonNode data
) {
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int SERVER_UNREACHABLE = -32000;

    public static McpError parseError(String msg) {
        return new McpError(PARSE_ERROR, msg, null);
    }

    public static McpError invalidRequest(String msg) {
        return new McpError(INVALID_REQUEST, msg, null);
    }

    public static McpError methodNotFound(String msg) {
        return new McpError(METHOD_NOT_FOUND, msg, null);
    }

    public static McpError invalidParams(String msg) {
        return new McpError(INVALID_PARAMS, msg, null);
    }

    public static McpError internalError(String msg) {
        return new McpError(INTERNAL_ERROR, msg, null);
    }

    public static McpError serverUnreachable(String msg) {
        return new McpError(SERVER_UNREACHABLE, msg, null);
    }
}

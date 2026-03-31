package com.guardian.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpResponse(
        @JsonProperty("jsonrpc") String jsonrpc,
        @JsonProperty("id") Object id,
        @JsonProperty("result") JsonNode result,
        @JsonProperty("error") McpError error
) {
    public McpResponse {
        if (jsonrpc == null) {
            jsonrpc = "2.0";
        }
    }

    public static McpResponse success(Object id, JsonNode result) {
        return new McpResponse("2.0", id, result, null);
    }

    public static McpResponse error(Object id, McpError error) {
        return new McpResponse("2.0", id, null, error);
    }

    @JsonIgnore
    public boolean isError() {
        return error != null;
    }
}

package com.guardian.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpRequest(
        @JsonProperty("jsonrpc") String jsonrpc,
        @JsonProperty("id") Object id,
        @JsonProperty("method") String method,
        @JsonProperty("params") JsonNode params
) {
    public McpRequest {
        if (jsonrpc == null) {
            jsonrpc = "2.0";
        }
    }

    public static McpRequest of(Object id, String method, JsonNode params) {
        return new McpRequest("2.0", id, method, params);
    }

    @JsonIgnore
    public boolean isNotification() {
        return id == null;
    }
}

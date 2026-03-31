package com.guardian.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guardian.core.model.McpError;
import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("McpRequest should default to jsonrpc 2.0")
    void requestDefaultJsonRpc() {
        var request = new McpRequest(null, 1, "test", null);
        assertEquals("2.0", request.jsonrpc());
    }

    @Test
    @DisplayName("McpRequest.of factory should work")
    void requestFactory() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("key", "value");
        var request = McpRequest.of(1, "tools/call", params);
        assertEquals("2.0", request.jsonrpc());
        assertEquals(1, request.id());
        assertEquals("tools/call", request.method());
    }

    @Test
    @DisplayName("McpRequest isNotification")
    void requestIsNotification() {
        var notification = McpRequest.of(null, "notification", null);
        assertTrue(notification.isNotification());
        var request = McpRequest.of(1, "method", null);
        assertFalse(request.isNotification());
    }

    @Test
    @DisplayName("McpResponse.success factory")
    void responseSuccess() {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", "ok");
        var response = McpResponse.success(1, result);
        assertEquals("2.0", response.jsonrpc());
        assertEquals(1, response.id());
        assertNotNull(response.result());
        assertNull(response.error());
        assertFalse(response.isError());
    }

    @Test
    @DisplayName("McpResponse.error factory")
    void responseError() {
        var error = McpError.internalError("something went wrong");
        var response = McpResponse.error(1, error);
        assertTrue(response.isError());
        assertEquals(-32603, response.error().code());
    }

    @Test
    @DisplayName("McpError code constants")
    void errorCodes() {
        assertEquals(-32700, McpError.PARSE_ERROR);
        assertEquals(-32600, McpError.INVALID_REQUEST);
        assertEquals(-32601, McpError.METHOD_NOT_FOUND);
        assertEquals(-32602, McpError.INVALID_PARAMS);
        assertEquals(-32603, McpError.INTERNAL_ERROR);
        assertEquals(-32000, McpError.SERVER_UNREACHABLE);
    }

    @Test
    @DisplayName("McpError factory methods")
    void errorFactories() {
        assertEquals(-32700, McpError.parseError("test").code());
        assertEquals(-32600, McpError.invalidRequest("test").code());
        assertEquals(-32601, McpError.methodNotFound("test").code());
        assertEquals(-32602, McpError.invalidParams("test").code());
        assertEquals(-32603, McpError.internalError("test").code());
        assertEquals(-32000, McpError.serverUnreachable("test").code());
    }

    @Test
    @DisplayName("McpRequest serialization roundtrip")
    void requestSerialization() throws Exception {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("tool", "query_db");
        var request = McpRequest.of(42, "tools/call", params);

        String json = objectMapper.writeValueAsString(request);
        var deserialized = objectMapper.readValue(json, McpRequest.class);

        assertEquals(request.jsonrpc(), deserialized.jsonrpc());
        assertEquals(request.method(), deserialized.method());
    }
}

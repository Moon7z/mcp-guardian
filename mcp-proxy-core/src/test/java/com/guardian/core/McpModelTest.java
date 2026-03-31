package com.guardian.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guardian.core.model.McpError;
import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;
import com.guardian.core.interceptor.InterceptorContext;
import com.guardian.core.interceptor.InterceptorChain;
import com.guardian.core.interceptor.McpInterceptor;
import com.guardian.core.router.McpRouter;
import com.guardian.core.config.GuardianProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
        assertEquals("query_db", deserialized.params().get("tool").asText());
    }

    // === InterceptorContext Tests ===
    @Nested
    @DisplayName("InterceptorContext")
    class InterceptorContextTest {

        @Test
        @DisplayName("Should store and retrieve typed attributes safely")
        void typedAttributeAccess() {
            var ctx = new InterceptorContext("s1", "srv1", "user1");
            ctx.setAttribute("policy.blocked", true);
            ctx.setAttribute("policy.rule", "block-sql");

            assertEquals(true, ctx.getAttribute("policy.blocked", Boolean.class));
            assertEquals("block-sql", ctx.getAttribute("policy.rule", String.class));
        }

        @Test
        @DisplayName("Should return null for missing attribute")
        void missingAttribute() {
            var ctx = new InterceptorContext("s1", "srv1", "user1");
            assertNull(ctx.getAttribute("nonexistent", String.class));
        }

        @Test
        @DisplayName("Should throw ClassCastException on type mismatch")
        void attributeTypeMismatch() {
            var ctx = new InterceptorContext("s1", "srv1", "user1");
            ctx.setAttribute("policy.blocked", true);
            assertThrows(ClassCastException.class,
                    () -> ctx.getAttribute("policy.blocked", String.class));
        }

        @Test
        @DisplayName("getUserRole falls back to userId when role is null")
        void userRoleFallback() {
            var ctx = new InterceptorContext("s1", "srv1", "alice");
            assertEquals("alice", ctx.getUserRole());

            var ctx2 = new InterceptorContext("s1", "srv1", "alice", "admin");
            assertEquals("admin", ctx2.getUserRole());
        }
    }

    // === InterceptorChain Tests ===
    @Nested
    @DisplayName("InterceptorChain")
    class InterceptorChainTest {

        @Test
        @DisplayName("Should execute pre-handlers in order")
        void preHandleOrder() {
            var order = new AtomicInteger(0);
            var interceptorA = new TestInterceptor(100, "A", order);
            var interceptorB = new TestInterceptor(200, "B", order);

            var chain = new InterceptorChain(List.of(interceptorB, interceptorA));
            var req = McpRequest.of(1, "test", null);
            var ctx = new InterceptorContext("s1", "srv1", "user1");

            chain.executePreHandle(req, ctx);
            assertEquals(1, interceptorA.preHandleCalledAt);
            assertEquals(2, interceptorB.preHandleCalledAt);
        }

        @Test
        @DisplayName("Should execute post-handlers in reverse order")
        void postHandleReverseOrder() {
            var order = new AtomicInteger(0);
            var interceptorA = new TestInterceptor(100, "A", order);
            var interceptorB = new TestInterceptor(200, "B", order);

            var chain = new InterceptorChain(List.of(interceptorA, interceptorB));
            var req = McpRequest.of(1, "test", null);
            var resp = McpResponse.success(1, null);
            var ctx = new InterceptorContext("s1", "srv1", "user1");

            chain.executePostHandle(req, resp, ctx);
            assertEquals(1, interceptorB.postHandleCalledAt);
            assertEquals(2, interceptorA.postHandleCalledAt);
        }

        private static class TestInterceptor implements McpInterceptor {
            final int orderVal;
            final String name;
            final AtomicInteger counter;
            int preHandleCalledAt = -1;
            int postHandleCalledAt = -1;

            TestInterceptor(int order, String name, AtomicInteger counter) {
                this.orderVal = order;
                this.name = name;
                this.counter = counter;
            }

            @Override
            public int getOrder() { return orderVal; }

            @Override
            public McpRequest preHandle(McpRequest request, InterceptorContext context) {
                preHandleCalledAt = counter.incrementAndGet();
                return request;
            }

            @Override
            public McpResponse postHandle(McpRequest request, McpResponse response, InterceptorContext context) {
                postHandleCalledAt = counter.incrementAndGet();
                return response;
            }
        }
    }

    // === McpRouter Tests ===
    @Nested
    @DisplayName("McpRouter")
    class McpRouterTest {

        @Test
        @DisplayName("Should resolve registered enabled server")
        void resolveEnabledServer() {
            var serverConfig = new GuardianProperties.ServerConfig(
                    "DB Tools", "http://localhost:3001", "http://localhost:3001/health", true);
            var props = new GuardianProperties(Map.of("db-tools", serverConfig), null);
            var router = new McpRouter(props);
            router.init();

            assertTrue(router.resolve("db-tools").isPresent());
            assertEquals("http://localhost:3001", router.resolve("db-tools").get().url());
        }

        @Test
        @DisplayName("Should not resolve disabled server")
        void disabledServerNotResolved() {
            var serverConfig = new GuardianProperties.ServerConfig(
                    "Code Tools", "http://localhost:3003", null, false);
            var props = new GuardianProperties(Map.of("code-tools", serverConfig), null);
            var router = new McpRouter(props);
            router.init();

            assertTrue(router.resolve("code-tools").isEmpty());
        }

        @Test
        @DisplayName("Should return empty for unknown server")
        void unknownServerEmpty() {
            var props = new GuardianProperties(Map.of(), null);
            var router = new McpRouter(props);
            router.init();

            assertTrue(router.resolve("nonexistent").isEmpty());
        }

        @Test
        @DisplayName("getAllServers returns immutable copy")
        void allServersImmutable() {
            var serverConfig = new GuardianProperties.ServerConfig(
                    "DB Tools", "http://localhost:3001", null, true);
            var props = new GuardianProperties(Map.of("db-tools", serverConfig), null);
            var router = new McpRouter(props);
            router.init();

            var servers = router.getAllServers();
            assertEquals(1, servers.size());
            assertThrows(UnsupportedOperationException.class,
                    () -> servers.put("new", null));
        }
    }
}

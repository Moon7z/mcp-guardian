package com.guardian.core.transport;

import com.guardian.core.exception.McpProxyException;
import com.guardian.core.model.McpError;
import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/mcp")
@Tag(name = "MCP Proxy", description = "MCP 协议代理端点，负责转发请求到下游 MCP 服务器")
public class McpProxyController {

    private static final Logger log = LoggerFactory.getLogger(McpProxyController.class);
    private final SseTransport sseTransport;

    public McpProxyController(SseTransport sseTransport) {
        this.sseTransport = sseTransport;
    }

    @Operation(summary = "建立 SSE 连接", description = "与指定的下游 MCP 服务器建立 Server-Sent Events 长连接")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 连接建立成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "服务器不存在")
    })
    @GetMapping(value = "/{serverId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sseConnect(
            @Parameter(description = "下游 MCP 服务器 ID") @PathVariable String serverId,
            @Parameter(description = "会话 ID") @RequestHeader(value = "X-Session-Id", defaultValue = "anonymous") String sessionId,
            @Parameter(description = "用户 ID") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {

        log.info("SSE connect request: server={}, session={}, user={}", serverId, sessionId, userId);
        return sseTransport.connectSse(serverId, sessionId, userId);
    }

    @Operation(summary = "转发 MCP 请求", description = "将 JSON-RPC 2.0 格式的 MCP 请求转发到指定的下游服务器，经过安全检查、DLP 脱敏和策略引擎")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "请求处理成功（包含 JSON-RPC 错误响应）"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "策略拒绝")
    })
    @PostMapping(value = "/{serverId}/message",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<McpResponse>> forwardMessage(
            @Parameter(description = "下游 MCP 服务器 ID") @PathVariable String serverId,
            @RequestBody McpRequest request,
            @Parameter(description = "会话 ID") @RequestHeader(value = "X-Session-Id", defaultValue = "anonymous") String sessionId,
            @Parameter(description = "用户 ID") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {

        log.info("Forward request: server={}, method={}, session={}", serverId, request.method(), sessionId);

        return sseTransport.forwardRequest(serverId, request, sessionId, userId)
                .map(ResponseEntity::ok)
                .onErrorResume(McpProxyException.class, ex ->
                        Mono.just(ResponseEntity.ok(McpResponse.error(request.id(), ex.getMcpError()))));
    }

    @Operation(summary = "健康检查", description = "返回服务健康状态，此端点无需认证")
    @ApiResponse(responseCode = "200", description = "服务正常")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\",\"service\":\"mcp-guardian\"}");
    }
}

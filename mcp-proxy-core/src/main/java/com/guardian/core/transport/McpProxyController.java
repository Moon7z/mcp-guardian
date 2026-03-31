package com.guardian.core.transport;

import com.guardian.core.exception.McpProxyException;
import com.guardian.core.model.McpError;
import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;
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
public class McpProxyController {

    private static final Logger log = LoggerFactory.getLogger(McpProxyController.class);
    private final SseTransport sseTransport;

    public McpProxyController(SseTransport sseTransport) {
        this.sseTransport = sseTransport;
    }

    @GetMapping(value = "/{serverId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sseConnect(
            @PathVariable String serverId,
            @RequestHeader(value = "X-Session-Id", defaultValue = "anonymous") String sessionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {

        log.info("SSE connect request: server={}, session={}, user={}", serverId, sessionId, userId);
        return sseTransport.connectSse(serverId, sessionId, userId);
    }

    @PostMapping(value = "/{serverId}/message",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<McpResponse>> forwardMessage(
            @PathVariable String serverId,
            @RequestBody McpRequest request,
            @RequestHeader(value = "X-Session-Id", defaultValue = "anonymous") String sessionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {

        log.info("Forward request: server={}, method={}, session={}", serverId, request.method(), sessionId);

        return sseTransport.forwardRequest(serverId, request, sessionId, userId)
                .map(ResponseEntity::ok)
                .onErrorResume(McpProxyException.class, ex ->
                        Mono.just(ResponseEntity.ok(McpResponse.error(request.id(), ex.getMcpError()))));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\",\"service\":\"mcp-guardian\"}");
    }
}

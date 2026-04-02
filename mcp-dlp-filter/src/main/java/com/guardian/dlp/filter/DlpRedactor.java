package com.guardian.dlp.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.guardian.dlp.pattern.SensitivePatterns;
import com.guardian.dlp.pattern.SensitivePatterns.SensitivePattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;

@Component
public class DlpRedactor {

    private static final Logger log = LoggerFactory.getLogger(DlpRedactor.class);
    private static final long SLOW_THRESHOLD_MS = 50;
    private final ObjectMapper objectMapper;

    public DlpRedactor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record RedactionResult(
            String redactedText,
            List<String> detectedTypes,
            Map<String, Integer> replacementCounts,
            long durationMs
    ) {}

    public RedactionResult redactText(String input) {
        long startTime = System.nanoTime();

        if (input == null || input.isEmpty()) {
            return new RedactionResult(input, List.of(), Map.of(), 0);
        }

        String result = input;
        List<String> detected = new ArrayList<>();
        Map<String, Integer> replacementCounts = new LinkedHashMap<>();

        for (SensitivePattern pattern : SensitivePatterns.ALL_PATTERNS) {
            Matcher matcher = pattern.pattern().matcher(result);
            int count = 0;
            while (matcher.find()) {
                count++;
            }

            if (count > 0) {
                detected.add(pattern.name());
                replacementCounts.put(pattern.name(), count);
                result = pattern.pattern().matcher(result).replaceAll(pattern.replacement());
            }
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        if (!detected.isEmpty()) {
            log.info("DLP redacted {} pattern(s): {}, replacements: {}, duration: {}ms",
                    detected.size(), detected, replacementCounts, durationMs);
        }

        return new RedactionResult(result, detected, replacementCounts, durationMs);
    }

    public JsonNode redactJsonNode(JsonNode node) {
        long startTime = System.nanoTime();
        JsonNode result = redactNodeRecursive(node);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        if (durationMs > SLOW_THRESHOLD_MS) {
            log.warn("DLP JSON redaction took {}ms (threshold: {}ms), node type: {}",
                    durationMs, SLOW_THRESHOLD_MS, node != null ? node.getNodeType() : "null");
        } else {
            log.debug("DLP JSON redaction completed in {}ms", durationMs);
        }

        return result;
    }

    private JsonNode redactNodeRecursive(JsonNode node) {
        if (node == null) {
            return null;
        }

        if (node.isTextual()) {
            RedactionResult result = redactText(node.asText());
            return new TextNode(result.redactedText());
        }

        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                objectNode.set(field.getKey(), redactNodeRecursive(field.getValue()));
            }
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(redactNodeRecursive(element));
            }
            return arrayNode;
        }

        return node;
    }

    public String redactJsonString(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        long startTime = System.nanoTime();

        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode redacted = redactJsonNode(node);
            String result = objectMapper.writeValueAsString(redacted);

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.debug("JSON string redaction completed in {}ms, input length: {}", durationMs, json.length());

            return result;
        } catch (JsonProcessingException e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Failed to parse JSON for DLP redaction (duration: {}ms, input length: {})",
                    durationMs, json.length(), e);
            return json;
        }
    }
}

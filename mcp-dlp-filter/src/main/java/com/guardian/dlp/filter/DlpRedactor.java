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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

@Component
public class DlpRedactor {

    private static final Logger log = LoggerFactory.getLogger(DlpRedactor.class);
    private final ObjectMapper objectMapper;

    public DlpRedactor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record RedactionResult(String redactedText, List<String> detectedTypes) {}

    public RedactionResult redactText(String input) {
        if (input == null || input.isEmpty()) {
            return new RedactionResult(input, List.of());
        }

        String result = input;
        List<String> detected = new ArrayList<>();

        for (SensitivePattern pattern : SensitivePatterns.ALL_PATTERNS) {
            Matcher matcher = pattern.pattern().matcher(result);
            if (matcher.find()) {
                detected.add(pattern.name());
                result = matcher.replaceAll(pattern.replacement());
            }
        }

        if (!detected.isEmpty()) {
            log.info("DLP redacted {} sensitive pattern(s): {}", detected.size(), detected);
        }

        return new RedactionResult(result, detected);
    }

    public JsonNode redactJsonNode(JsonNode node) {
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
                objectNode.set(field.getKey(), redactJsonNode(field.getValue()));
            }
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(redactJsonNode(element));
            }
            return arrayNode;
        }

        return node;
    }

    public String redactJsonString(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode redacted = redactJsonNode(node);
            return objectMapper.writeValueAsString(redacted);
        } catch (JsonProcessingException e) {
            return redactText(json).redactedText();
        }
    }
}

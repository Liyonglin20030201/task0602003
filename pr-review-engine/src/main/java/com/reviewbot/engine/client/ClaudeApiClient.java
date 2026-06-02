package com.reviewbot.engine.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewbot.core.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClaudeApiClient {

    private final WebClient webClient;
    private final AppProperties.Claude claudeConfig;
    private final ObjectMapper objectMapper;

    public ClaudeApiClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.claudeConfig = appProperties.getClaude();
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(claudeConfig.getBaseUrl())
                .defaultHeader("x-api-key", claudeConfig.getApiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String reviewCode(String diffContent, String prTitle) {
        Map<String, Object> requestBody = Map.of(
                "model", claudeConfig.getModel(),
                "max_tokens", claudeConfig.getMaxTokens(),
                "system", buildSystemPrompt(),
                "messages", List.of(
                        Map.of("role", "user", "content", buildUserPrompt(diffContent, prTitle))
                )
        );

        String response = webClient.post()
                .uri("/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryable))
                .block(Duration.ofMinutes(2));

        return extractTextContent(response);
    }

    private String buildSystemPrompt() {
        return """
                You are a senior code reviewer. Analyze the pull request diff provided and give specific, actionable feedback.

                For each issue found, respond in JSON format:
                {
                  "summary": "Brief overall assessment",
                  "comments": [
                    {
                      "filePath": "path/to/file",
                      "lineNumber": 42,
                      "severity": "error|warning|suggestion",
                      "message": "Description of the issue",
                      "suggestion": "How to fix it"
                    }
                  ]
                }

                Focus on: bugs, security vulnerabilities, performance issues, code style, and best practices.
                Only report meaningful issues. Do not nitpick formatting unless it impacts readability significantly.
                """;
    }

    private String buildUserPrompt(String diffContent, String prTitle) {
        return "PR Title: " + prTitle + "\n\nDiff:\n```diff\n" + diffContent + "\n```";
    }

    private boolean isRetryable(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null && (message.contains("429") || message.contains("500") || message.contains("503"));
    }

    private String extractTextContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.get("content");
            if (content != null && content.isArray() && !content.isEmpty()) {
                return content.get(0).get("text").asText();
            }
        } catch (Exception e) {
            log.error("Failed to parse Claude API response", e);
        }
        return response;
    }
}

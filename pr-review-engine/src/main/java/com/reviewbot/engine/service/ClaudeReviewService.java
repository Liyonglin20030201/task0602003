package com.reviewbot.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewbot.core.model.ReviewResult;
import com.reviewbot.engine.client.ClaudeApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeReviewService {

    private final ClaudeApiClient claudeApiClient;
    private final ObjectMapper objectMapper;

    public ReviewResult analyze(String diffContent, String prTitle) {
        String rawResponse = claudeApiClient.reviewCode(diffContent, prTitle);
        return parseReviewResponse(rawResponse);
    }

    private ReviewResult parseReviewResponse(String rawResponse) {
        try {
            String jsonContent = extractJson(rawResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            String summary = root.has("summary") ? root.get("summary").asText() : "Review completed";
            List<ReviewResult.ReviewComment> comments = new ArrayList<>();

            JsonNode commentsNode = root.get("comments");
            if (commentsNode != null && commentsNode.isArray()) {
                for (JsonNode c : commentsNode) {
                    comments.add(ReviewResult.ReviewComment.builder()
                            .filePath(c.has("filePath") ? c.get("filePath").asText() : null)
                            .lineNumber(c.has("lineNumber") ? c.get("lineNumber").asInt() : null)
                            .severity(c.has("severity") ? c.get("severity").asText() : "suggestion")
                            .message(c.has("message") ? c.get("message").asText() : "")
                            .suggestion(c.has("suggestion") ? c.get("suggestion").asText() : null)
                            .build());
                }
            }

            return ReviewResult.builder()
                    .summary(summary)
                    .comments(comments)
                    .build();
        } catch (Exception e) {
            log.warn("Could not parse structured response, using raw text", e);
            return ReviewResult.builder()
                    .summary(rawResponse)
                    .comments(List.of())
                    .build();
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}

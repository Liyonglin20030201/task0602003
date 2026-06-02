package com.reviewbot.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {
    private String reviewId;
    private String summary;
    private List<ReviewComment> comments;
    private SandboxResult sandboxResult;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewComment {
        private String filePath;
        private Integer lineNumber;
        private String severity;
        private String message;
        private String suggestion;
    }
}

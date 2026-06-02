package com.reviewbot.dashboard.dto;

import com.reviewbot.core.model.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressMessage {
    private String reviewId;
    private String repoFullName;
    private Integer prNumber;
    private ReviewStatus status;
    private String details;
    private Instant timestamp;
}

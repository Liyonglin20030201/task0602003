package com.reviewbot.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest implements Serializable {
    private String reviewId;
    private String repoFullName;
    private String repoOwner;
    private String repoName;
    private Integer prNumber;
    private String headSha;
    private String diffUrl;
    private String prTitle;
    private String prAuthor;
    private Instant createdAt;
}

package com.reviewbot.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestEvent {
    private String action;
    private Long pullRequestId;
    private Integer prNumber;
    private String repoFullName;
    private String repoOwner;
    private String repoName;
    private String headSha;
    private String baseBranch;
    private String headBranch;
    private String diffUrl;
    private String prTitle;
    private String prAuthor;
}

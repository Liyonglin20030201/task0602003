package com.reviewbot.core.model;

public enum ReviewStatus {
    QUEUED,
    CLONING_REPO,
    FETCHING_DIFF,
    ANALYZING,
    SANDBOXING,
    POSTING,
    COMPLETED,
    FAILED
}

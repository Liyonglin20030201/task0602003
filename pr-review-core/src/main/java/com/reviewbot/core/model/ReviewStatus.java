package com.reviewbot.core.model;

public enum ReviewStatus {
    QUEUED,
    FETCHING_DIFF,
    ANALYZING,
    SANDBOXING,
    POSTING,
    COMPLETED,
    FAILED
}

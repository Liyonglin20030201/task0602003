package com.reviewbot.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxResult {
    private int exitCode;
    private String stdout;
    private String stderr;
    private boolean timedOut;
    private long executionTimeMs;
}

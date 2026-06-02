package com.reviewbot.sandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {
    private String code;
    private String className;
    @Builder.Default
    private int timeoutSeconds = 10;
}

package com.reviewbot.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "sandbox_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_record_id", nullable = false)
    @JsonIgnore
    private ReviewRecord reviewRecord;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String className;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus executionStatus;

    private int exitCode;

    @Column(columnDefinition = "TEXT")
    private String stdout;

    @Column(columnDefinition = "TEXT")
    private String stderr;

    private boolean timedOut;

    private boolean compilationFailed;

    private long executionTimeMs;

    private Instant executedAt;

    public enum ExecutionStatus {
        SUCCESS,
        COMPILATION_ERROR,
        RUNTIME_ERROR,
        TIMEOUT,
        SANDBOX_ERROR
    }
}

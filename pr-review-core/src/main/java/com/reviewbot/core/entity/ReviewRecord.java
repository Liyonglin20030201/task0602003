package com.reviewbot.core.entity;

import com.reviewbot.core.model.ReviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecord {

    @Id
    private String id;

    @Column(nullable = false)
    private String repoFullName;

    @Column(nullable = false)
    private Integer prNumber;

    private String prTitle;
    private String prAuthor;
    private String headSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "reviewRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewCommentEntity> comments = new ArrayList<>();

    @OneToMany(mappedBy = "reviewRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SandboxExecutionEntity> sandboxExecutions = new ArrayList<>();

    private Instant createdAt;
    private Instant completedAt;
}

package com.reviewbot.core.repository;

import com.reviewbot.core.entity.ReviewRecord;
import com.reviewbot.core.model.ReviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, String> {
    List<ReviewRecord> findByRepoFullNameOrderByCreatedAtDesc(String repoFullName);
    List<ReviewRecord> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
    List<ReviewRecord> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"comments", "sandboxExecutions"})
    Optional<ReviewRecord> findWithDetailsById(String id);
}

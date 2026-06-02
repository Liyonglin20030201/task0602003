package com.reviewbot.core.repository;

import com.reviewbot.core.entity.ReviewRecord;
import com.reviewbot.core.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, String> {
    List<ReviewRecord> findByRepoFullNameOrderByCreatedAtDesc(String repoFullName);
    List<ReviewRecord> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
    List<ReviewRecord> findAllByOrderByCreatedAtDesc();
}

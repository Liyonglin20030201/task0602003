package com.reviewbot.dashboard.controller;

import com.reviewbot.core.entity.ReviewRecord;
import com.reviewbot.core.repository.ReviewRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class DashboardRestController {

    private final ReviewRecordRepository reviewRecordRepository;

    @GetMapping
    public List<ReviewRecord> listReviews() {
        return reviewRecordRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewRecord> getReview(@PathVariable String id) {
        return reviewRecordRepository.findWithDetailsById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

package com.reviewbot.engine.service;

import com.reviewbot.core.event.ReviewProgressEvent;
import com.reviewbot.core.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ProgressNotifier {

    private final ApplicationEventPublisher eventPublisher;

    public void notify(String reviewId, String repoFullName, Integer prNumber,
                       ReviewStatus status, String details) {
        eventPublisher.publishEvent(ReviewProgressEvent.builder()
                .reviewId(reviewId)
                .repoFullName(repoFullName)
                .prNumber(prNumber)
                .status(status)
                .details(details)
                .timestamp(Instant.now())
                .build());
    }
}

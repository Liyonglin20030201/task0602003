package com.reviewbot.dashboard.listener;

import com.reviewbot.core.event.ReviewProgressEvent;
import com.reviewbot.dashboard.dto.ProgressMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewProgressListener {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onReviewProgress(ReviewProgressEvent event) {
        ProgressMessage message = ProgressMessage.builder()
                .reviewId(event.getReviewId())
                .repoFullName(event.getRepoFullName())
                .prNumber(event.getPrNumber())
                .status(event.getStatus())
                .details(event.getDetails())
                .timestamp(event.getTimestamp())
                .build();

        messagingTemplate.convertAndSend("/topic/reviews/" + event.getReviewId(), message);
        messagingTemplate.convertAndSend("/topic/reviews/all", message);

        log.debug("Broadcast progress: {} -> {}", event.getReviewId(), event.getStatus());
    }
}

package com.reviewbot.engine.consumer;

import com.reviewbot.core.config.RabbitMQConfig;
import com.reviewbot.core.model.ReviewRequest;
import com.reviewbot.engine.service.ReviewOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRequestConsumer {

    private final ReviewOrchestrator reviewOrchestrator;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_QUEUE)
    public void consume(ReviewRequest request) {
        log.info("Consuming review request: {} for {}#{}",
                request.getReviewId(), request.getRepoFullName(), request.getPrNumber());
        reviewOrchestrator.executeReview(request);
    }
}

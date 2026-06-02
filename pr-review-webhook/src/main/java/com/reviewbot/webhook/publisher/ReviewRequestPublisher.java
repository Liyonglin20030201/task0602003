package com.reviewbot.webhook.publisher;

import com.reviewbot.core.config.RabbitMQConfig;
import com.reviewbot.core.model.PullRequestEvent;
import com.reviewbot.core.model.ReviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRequestPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(PullRequestEvent event) {
        ReviewRequest request = ReviewRequest.builder()
                .reviewId(UUID.randomUUID().toString())
                .repoFullName(event.getRepoFullName())
                .repoOwner(event.getRepoOwner())
                .repoName(event.getRepoName())
                .prNumber(event.getPrNumber())
                .headSha(event.getHeadSha())
                .diffUrl(event.getDiffUrl())
                .prTitle(event.getPrTitle())
                .prAuthor(event.getPrAuthor())
                .createdAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REVIEW_EXCHANGE,
                RabbitMQConfig.REVIEW_ROUTING_KEY,
                request);

        log.info("Published review request: {} for {}#{}", request.getReviewId(), event.getRepoFullName(), event.getPrNumber());
    }
}

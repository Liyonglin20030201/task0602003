package com.reviewbot.webhook.controller;

import com.reviewbot.core.config.AppProperties;
import com.reviewbot.core.util.HmacUtils;
import com.reviewbot.webhook.service.PayloadParserService;
import com.reviewbot.webhook.publisher.ReviewRequestPublisher;
import com.reviewbot.core.model.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private final AppProperties appProperties;
    private final PayloadParserService payloadParser;
    private final ReviewRequestPublisher publisher;

    @PostMapping("/github")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestBody String payload) {

        if (!HmacUtils.verifyGitHubSignature(payload, signature, appProperties.getGithub().getWebhookSecret())) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        if (!"pull_request".equals(event)) {
            log.debug("Ignoring non-PR event: {}", event);
            return ResponseEntity.ok("Event ignored");
        }

        PullRequestEvent prEvent = payloadParser.parse(payload);
        if (prEvent == null) {
            return ResponseEntity.ok("Action ignored");
        }

        if (!"opened".equals(prEvent.getAction()) && !"synchronize".equals(prEvent.getAction())) {
            log.debug("Ignoring PR action: {}", prEvent.getAction());
            return ResponseEntity.ok("Action ignored");
        }

        log.info("Received PR event: {}#{} action={}", prEvent.getRepoFullName(), prEvent.getPrNumber(), prEvent.getAction());
        publisher.publish(prEvent);

        return ResponseEntity.accepted().body("Review queued");
    }
}

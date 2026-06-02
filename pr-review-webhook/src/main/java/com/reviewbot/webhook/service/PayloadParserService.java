package com.reviewbot.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewbot.core.model.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayloadParserService {

    private final ObjectMapper objectMapper;

    public PullRequestEvent parse(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode pr = root.get("pull_request");
            JsonNode repo = root.get("repository");

            if (pr == null || repo == null) {
                log.warn("Missing pull_request or repository in payload");
                return null;
            }

            return PullRequestEvent.builder()
                    .action(root.get("action").asText())
                    .pullRequestId(pr.get("id").asLong())
                    .prNumber(pr.get("number").asInt())
                    .repoFullName(repo.get("full_name").asText())
                    .repoOwner(repo.get("owner").get("login").asText())
                    .repoName(repo.get("name").asText())
                    .headSha(pr.get("head").get("sha").asText())
                    .baseBranch(pr.get("base").get("ref").asText())
                    .headBranch(pr.get("head").get("ref").asText())
                    .diffUrl(pr.get("diff_url").asText())
                    .prTitle(pr.get("title").asText())
                    .prAuthor(pr.get("user").get("login").asText())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse webhook payload", e);
            return null;
        }
    }
}
